package com.diving.base.service;

import com.diving.base.dto.request.SeaConditionReportRequest;
import com.diving.base.dto.response.PageResponse;
import com.diving.base.dto.response.SeaConditionReportResponse;
import com.diving.base.entity.SeaConditionReport;
import com.diving.base.repository.SeaConditionReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 每日海况单填报。
 *
 * 一天只能落一张单：同日期的提交先过 Redis 锁串行化，事务内再查重，
 * 最后由 uk_report_date 唯一索引兜底——两个人同一天同时交单，
 * 数据库层面也只允许一条落下，后到的人拿到“今天已经记过”的提示。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeaConditionService {

    private static final String REPORT_LOCK_PREFIX = "lock:sea:report:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final String DEFAULT_REPORTER = "岸上人员";

    private final SeaConditionReportRepository seaConditionReportRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private SeaConditionService self;

    @Autowired
    public void setSelf(@Lazy SeaConditionService self) {
        this.self = self;
    }

    /**
     * 交海况单入口（不加事务）：对同一天的提交加 Redis 互斥锁，锁覆盖整个事务直到提交完成。
     * 抢不到锁或事务内撞唯一索引，都说明今天的海况单已经落下，
     * 需把当天已记的海况单带回，让后到的人看出今天已经记过。
     */
    public SeaConditionReportResponse report(SeaConditionReportRequest request) {
        LocalDate date = request.getReportDate() != null ? request.getReportDate() : LocalDate.now();
        String lockKey = REPORT_LOCK_PREFIX + date;
        boolean locked = false;
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_TTL);
            locked = Boolean.TRUE.equals(acquired);
            if (!locked) {
                log.warn("海况单并发提交被锁拦截: date={}", date);
                // 持锁者提交只是毫秒级：短暂等待后回查，让后到的人看到当天已记下的
                // 浪高、能见度；超过等待窗口仍读不到（持锁者异常）才退化为“提交中”提示
                throw new RuntimeException(reportExistsMessageAfterCommit(date));
            }
            return new SeaConditionReportResponse(self.doReport(request, date));
        } catch (DataIntegrityViolationException e) {
            // uk_report_date 唯一索引兜底：两人同天同时交单只有一张能落库
            log.warn("唯一约束兜底拦截重复海况单: date={}", date);
            throw new RuntimeException(reportExistsMessage(date));
        } finally {
            if (locked) {
                try {
                    stringRedisTemplate.delete(lockKey);
                } catch (Exception ex) {
                    log.warn("释放海况单防重锁失败，等待TTL自动过期: key={}", lockKey, ex);
                }
            }
        }
    }

    @Transactional
    public SeaConditionReport doReport(SeaConditionReportRequest request, LocalDate date) {
        seaConditionReportRepository.findByReportDate(date)
                .ifPresent(existing -> {
                    throw new RuntimeException(buildReportExistsMessage(existing));
                });

        SeaConditionReport report = SeaConditionReport.builder()
                .reportDate(date)
                .waveHeight(request.getWaveHeight())
                .visibility(request.getVisibility())
                .divable(request.getDivable() != null ? request.getDivable() : SeaConditionReport.DIVABLE)
                .reporter(request.getReporter() != null && !request.getReporter().isBlank()
                        ? request.getReporter().trim() : DEFAULT_REPORTER)
                .remark(request.getRemark())
                .build();

        // flush 让唯一约束冲突在此事务内抛出，由上层统一转成已填报提示
        return seaConditionReportRepository.saveAndFlush(report);
    }

    /** 某天的海况单；当天未交单时返回 null */
    public SeaConditionReportResponse findByDate(LocalDate date) {
        return seaConditionReportRepository.findByReportDate(date)
                .map(SeaConditionReportResponse::new)
                .orElse(null);
    }

    /**
     * 开潜判定：返回开潜日期当天的海况单，不存在时为 null（当天未交单，不拦截）。
     * 直读数据库，不走缓存，保证岸上刚记下不能下水，开潜立刻被挡住。
     */
    public Optional<SeaConditionReport> findEntityByDate(LocalDate date) {
        return seaConditionReportRepository.findByReportDate(date);
    }

    /** 海况单历史列表，按日期倒序，不走缓存 */
    public PageResponse<SeaConditionReportResponse> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SeaConditionReportResponse> resultPage = seaConditionReportRepository
                .findAllByOrderByReportDateDesc(pageable)
                .map(SeaConditionReportResponse::new);
        return PageResponse.from(resultPage);
    }

    /**
     * 组装当天已填报提示：必须带出海况单上的浪高、能见度与能否下水，
     * 让后到的人看出今天已经记过，而不是只看到一个笼统的失败。
     */
    private String reportExistsMessage(LocalDate date) {
        return seaConditionReportRepository.findByReportDate(date)
                .map(this::buildReportExistsMessage)
                .orElse(String.format("%s 的海况单正在提交中，请勿重复交单", date));
    }

    /**
     * 锁竞争失败时使用：持锁线程的事务通常在毫秒内提交，
     * 短暂等待后回查，尽量把已落库海况单的浪高、能见度带给后到的人。
     */
    private String reportExistsMessageAfterCommit(LocalDate date) {
        for (int i = 0; i < 10; i++) {
            var existing = seaConditionReportRepository.findByReportDate(date);
            if (existing.isPresent()) {
                return buildReportExistsMessage(existing.get());
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return String.format("%s 的海况单正在提交中，请勿重复交单", date);
    }

    private String buildReportExistsMessage(SeaConditionReport existing) {
        return String.format(
                "%s 的海况单今天已经记过：浪高 %s 米、能见度 %s 米、%s（填报人：%s），每天只能交一张",
                existing.getReportDate(),
                formatMetric(existing.getWaveHeight()),
                formatMetric(existing.getVisibility()),
                Boolean.FALSE.equals(existing.getDivable()) ? "不能下水" : "可以下水",
                existing.getReporter() != null ? existing.getReporter() : DEFAULT_REPORTER);
    }

    private String formatMetric(BigDecimal value) {
        return value != null ? value.stripTrailingZeros().toPlainString() : "—";
    }
}
