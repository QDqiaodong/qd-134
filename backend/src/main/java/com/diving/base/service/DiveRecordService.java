package com.diving.base.service;

import com.diving.base.dto.request.DiveEndRequest;
import com.diving.base.dto.request.DiveStartRequest;
import com.diving.base.dto.response.DiveRecordResponse;
import com.diving.base.dto.response.PageResponse;
import com.diving.base.entity.DiveRecord;
import com.diving.base.entity.Team;
import com.diving.base.repository.DiveRecordRepository;
import com.diving.base.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 下潜记录：开潜（开始+预计结束）与收潜（实际结束）。
 *
 * 互斥策略与绑定提交一致：同一小组的开潜请求先过 Redis 锁串行化，
 * 事务内再查重，最后由 dive_record.uk_active_team 唯一索引兜底——
 * 两人同时给同一组开潜，数据库层面也只允许一条未收潜记录落下。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiveRecordService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String START_LOCK_PREFIX = "lock:dive:start:";
    private static final String END_LOCK_PREFIX = "lock:dive:end:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final DiveRecordRepository diveRecordRepository;
    private final TeamRepository teamRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private DiveRecordService self;

    @Autowired
    public void setSelf(@Lazy DiveRecordService self) {
        this.self = self;
    }

    /**
     * 开潜入口（不加事务）：对同一小组的开潜提交加 Redis 互斥锁，锁覆盖整个事务直到提交完成。
     * 抢不到锁或事务内撞唯一索引，都说明该小组仍有未收潜记录，
     * 需把上一潜的开始时刻写进提示返回。
     */
    public DiveRecordResponse start(DiveStartRequest request) {
        String lockKey = START_LOCK_PREFIX + request.getTeamId();
        boolean locked = false;
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_TTL);
            locked = Boolean.TRUE.equals(acquired);
            if (!locked) {
                log.warn("开潜并发提交被锁拦截: teamId={}", request.getTeamId());
                throw new RuntimeException(openDiveBlockMessage(request.getTeamId()));
            }
            return self.doStart(request);
        } catch (DataIntegrityViolationException e) {
            // uk_active_team 唯一索引兜底：两人同时开潜时只有一条能落库
            log.warn("唯一约束兜底拦截并发开潜: teamId={}", request.getTeamId());
            throw new RuntimeException(openDiveBlockMessage(request.getTeamId()));
        } finally {
            if (locked) {
                try {
                    stringRedisTemplate.delete(lockKey);
                } catch (Exception ex) {
                    log.warn("释放开潜防重锁失败，等待TTL自动过期: key={}", lockKey, ex);
                }
            }
        }
    }

    @Transactional
    @CacheEvict(value = {"team", "teamList"}, allEntries = true)
    public DiveRecordResponse doStart(DiveStartRequest request) {
        // 直读数据库判定，不依赖可能过期的小组缓存
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RuntimeException("小组不存在: " + request.getTeamId()));

        if (!request.getPlannedEndTime().isAfter(request.getStartTime())) {
            throw new RuntimeException("预计结束时刻必须晚于开始时刻");
        }

        diveRecordRepository.findByActiveTeamId(request.getTeamId())
                .ifPresent(open -> {
                    throw new RuntimeException(buildOpenBlockMessage(team.getName(), open.getStartTime()));
                });

        DiveRecord record = DiveRecord.builder()
                .teamId(request.getTeamId())
                .startTime(request.getStartTime())
                .plannedEndTime(request.getPlannedEndTime())
                .status(DiveRecord.STATUS_OPEN)
                // 冗余写入互斥列：OPEN 记录与小组一一对应，收潜后置 null
                .activeTeamId(request.getTeamId())
                .build();

        return toResponse(diveRecordRepository.saveAndFlush(record), team.getName());
    }

    /**
     * 收潜入口（不加事务）：同一条记录的重复收潜提交用 Redis 锁串行化。
     */
    public DiveRecordResponse end(Long id, DiveEndRequest request) {
        String lockKey = END_LOCK_PREFIX + id;
        boolean locked = false;
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_TTL);
            locked = Boolean.TRUE.equals(acquired);
            if (!locked) {
                throw new RuntimeException("该下潜记录正在收潜，请勿重复提交");
            }
            return self.doEnd(id, request);
        } finally {
            if (locked) {
                try {
                    stringRedisTemplate.delete(lockKey);
                } catch (Exception ex) {
                    log.warn("释放收潜防重锁失败，等待TTL自动过期: key={}", lockKey, ex);
                }
            }
        }
    }

    @Transactional
    @CacheEvict(value = {"team", "teamList"}, allEntries = true)
    public DiveRecordResponse doEnd(Long id, DiveEndRequest request) {
        DiveRecord record = diveRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("下潜记录不存在: " + id));

        // 已收潜记录保持已收潜，不允许重复收潜或改回未收潜
        if (DiveRecord.STATUS_CLOSED.equals(record.getStatus())) {
            throw new RuntimeException("该下潜记录已于 "
                    + (record.getActualEndTime() != null
                        ? record.getActualEndTime().format(TIME_FORMATTER) : "")
                    + " 收潜，不能重复收潜");
        }

        if (request.getActualEndTime().isBefore(record.getStartTime())) {
            throw new RuntimeException(String.format(
                    "实际结束时刻(%s)不能早于开始时刻(%s)",
                    request.getActualEndTime().format(TIME_FORMATTER),
                    record.getStartTime().format(TIME_FORMATTER)));
        }

        record.setActualEndTime(request.getActualEndTime());
        record.setStatus(DiveRecord.STATUS_CLOSED);
        // 释放小组的未收潜互斥列，收潜后该小组才能开下一潜
        record.setActiveTeamId(null);

        Team team = teamRepository.findById(record.getTeamId()).orElse(null);
        return toResponse(diveRecordRepository.saveAndFlush(record),
                team != null ? team.getName() : null);
    }

    /**
     * 下潜记录列表不走缓存，未收潜/超时标记实时计算。
     */
    public PageResponse<DiveRecordResponse> findAll(int page, int size, Long teamId, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));
        Page<DiveRecordResponse> recordPage = diveRecordRepository.findRecordViews(teamId, status, pageable);
        return PageResponse.from(recordPage);
    }

    /** 小组当前未收潜记录；没有时返回 null，供小组卡片/列表展示在潜状态 */
    public DiveRecordResponse findActiveByTeam(Long teamId) {
        Optional<DiveRecord> open = diveRecordRepository.findByActiveTeamId(teamId);
        if (open.isEmpty()) {
            return null;
        }
        Team team = teamRepository.findById(teamId).orElse(null);
        return toResponse(open.get(), team != null ? team.getName() : null);
    }

    /**
     * 组装互斥提示：明确告知上一潜从几点开始、至今未收潜。
     */
    private String openDiveBlockMessage(Long teamId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        String teamName = team != null ? team.getName() : String.valueOf(teamId);
        return diveRecordRepository.findByActiveTeamId(teamId)
                .map(open -> buildOpenBlockMessage(teamName, open.getStartTime()))
                .orElse(String.format("小组[%s]当前下潜正在处理中，请勿重复提交", teamName));
    }

    private String buildOpenBlockMessage(String teamName, java.time.LocalDateTime startTime) {
        return String.format("小组[%s]上一潜从 %s 开始，尚未收潜，不能再开第二条下潜记录",
                teamName, startTime.format(TIME_FORMATTER));
    }

    private DiveRecordResponse toResponse(DiveRecord record, String teamName) {
        return new DiveRecordResponse(
                record.getId(),
                record.getTeamId(),
                teamName,
                record.getStartTime(),
                record.getPlannedEndTime(),
                record.getActualEndTime(),
                record.getStatus(),
                record.getCreatedAt());
    }
}
