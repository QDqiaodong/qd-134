package com.diving.base.service;

import com.diving.base.dto.request.BindingCreateRequest;
import com.diving.base.dto.request.BindingSyncRequest;
import com.diving.base.dto.response.BindingResponse;
import com.diving.base.dto.response.PageResponse;
import com.diving.base.entity.Binding;
import com.diving.base.entity.DepthAdjustRecord;
import com.diving.base.entity.Equipment;
import com.diving.base.entity.Team;
import com.diving.base.repository.BindingRepository;
import com.diving.base.repository.DepthAdjustRecordRepository;
import com.diving.base.repository.EquipmentRepository;
import com.diving.base.repository.TeamRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BindingService {

    private final BindingRepository bindingRepository;
    private final TeamService teamService;
    private final EquipmentService equipmentService;
    private final EquipmentRepository equipmentRepository;
    private final TeamRepository teamRepository;
    private final DepthValidationService depthValidationService;
    private final DepthAdjustRecordRepository depthAdjustRecordRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private BindingService self;

    @Autowired
    public void setSelf(@Lazy BindingService self) {
        this.self = self;
    }

    /**
     * 同小组+同装备的绑定提交互斥锁，锁需在事务提交后才释放，避免后来者
     * 在首条占用尚未落库可见前通过查重再插一条。锁 TTL 仅作为宕机兜底。
     */
    private static final String BIND_LOCK_PREFIX = "lock:binding:create:";
    private static final Duration BIND_LOCK_TTL = Duration.ofSeconds(10);

    @Cacheable(value = "binding", key = "#id")
    public Binding findById(Long id) {
        return bindingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("绑定关系不存在: " + id));
    }

    /**
     * 绑定记录列表不走缓存，超证标记按当前持证深度实时计算，
     * 保证刷新后超证标记与最新证深一致。
     */
    public PageResponse<BindingResponse> findAll(int page, int size, Boolean overCertified) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BindingResponse> bindingPage = bindingRepository.findBindingViews(overCertified, pageable);
        return PageResponse.from(bindingPage);
    }

    @Cacheable(value = "bindingTeam", key = "#teamId")
    public List<Binding> findByTeamId(Long teamId) {
        return bindingRepository.findByTeamIdAndStatus(teamId, "ACTIVE");
    }

    public List<Equipment> findEquipmentsByTeamId(Long teamId) {
        List<Long> equipmentIds = bindingRepository.findEquipmentIdsByTeamId(teamId);
        if (equipmentIds.isEmpty()) {
            return new ArrayList<>();
        }
        return equipmentRepository.findAllById(equipmentIds);
    }

    /**
     * 绑定入口（不加事务）：对同一「小组+装备」的提交加 Redis 互斥锁串行化，
     * 防连点/重放产生重复占用。锁覆盖整个事务直到提交完成才释放；
     * 抢不到锁说明同一提交已在处理中，直接按重复提交提示，不再开第二条占用。
     */
    public Binding create(BindingCreateRequest request) {
        String lockKey = BIND_LOCK_PREFIX + request.getTeamId() + ":" + request.getEquipmentId();
        boolean locked = false;
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", BIND_LOCK_TTL);
            locked = Boolean.TRUE.equals(acquired);
            if (!locked) {
                log.warn("绑定请求重复提交被拦截: teamId={}, equipmentId={}",
                        request.getTeamId(), request.getEquipmentId());
                throw new RuntimeException("该装备已绑定到该小组，请勿重复提交");
            }
            // 经代理调用，保证 @Transactional/@CacheEvict 生效且事务在锁内提交
            return self.doCreate(request);
        } catch (DataIntegrityViolationException e) {
            log.warn("唯一约束兜底拦截重复绑定: teamId={}, equipmentId={}",
                    request.getTeamId(), request.getEquipmentId());
            throw new RuntimeException("该装备已绑定到该小组，请勿重复提交");
        } finally {
            if (locked) {
                try {
                    stringRedisTemplate.delete(lockKey);
                } catch (Exception ex) {
                    log.warn("释放绑定防重锁失败，等待TTL自动过期: key={}", lockKey, ex);
                }
            }
        }
    }

    @Transactional
    @CacheEvict(value = {"binding", "bindingTeam", "teamList"}, allEntries = true)
    public Binding doCreate(BindingCreateRequest request) {
        Team team = teamService.findById(request.getTeamId());
        Equipment equipment = equipmentService.findById(request.getEquipmentId());

        if (bindingRepository.existsByTeamIdAndEquipmentId(request.getTeamId(), request.getEquipmentId())) {
            throw new RuntimeException("该装备已绑定到该小组，请勿重复提交");
        }

        depthValidationService.validateBinding(team, equipment);

        Binding binding = Binding.builder()
                .teamId(request.getTeamId())
                .equipmentId(request.getEquipmentId())
                .status("ACTIVE")
                .build();

        // flush 让唯一约束冲突在此事务内抛出，由上层统一转成重复提交提示
        return bindingRepository.saveAndFlush(binding);
    }

    @Transactional
    @CacheEvict(value = {"binding", "bindingTeam", "teamList"}, allEntries = true)
    public void delete(Long id) {
        Binding binding = findById(id);
        bindingRepository.delete(binding);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    @CacheEvict(value = {"binding", "bindingTeam", "teamList"}, allEntries = true)
    public Map<String, Object> syncDepth(BindingSyncRequest request) {
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RuntimeException("小组不存在: " + request.getTeamId()));
        Integer oldMaxDepth = team.getMaxDepth();
        Integer newMaxDepth = request.getNewMaxDepth();

        if (newMaxDepth > team.getCertifiedDepth()) {
            log.warn("同步深度拦截: 目标深度{}m超过小组[{}]持证深度{}m",
                    newMaxDepth, team.getName(), team.getCertifiedDepth());
            throw new RuntimeException(String.format(
                    "目标深度(%dm)超过小组[%s]持证深度(%dm)，证深不符禁止同步",
                    newMaxDepth, team.getName(), team.getCertifiedDepth()));
        }

        if (newMaxDepth >= oldMaxDepth) {
            return Map.of(
                    "removedCount", 0,
                    "removedEquipments", new ArrayList<>()
            );
        }

        List<Long> boundEquipmentIds = bindingRepository.findEquipmentIdsByTeamId(request.getTeamId());
        if (boundEquipmentIds.isEmpty()) {
            return Map.of(
                    "removedCount", 0,
                    "removedEquipments", new ArrayList<>()
            );
        }

        List<Equipment> overDepthEquipments = equipmentRepository
                .findByIdsAndMaxDepthGreaterThan(boundEquipmentIds, newMaxDepth);

        List<Long> removedEquipmentIds = new ArrayList<>();
        for (Equipment equipment : overDepthEquipments) {
            removedEquipmentIds.add(equipment.getId());
        }

        if (!removedEquipmentIds.isEmpty()) {
            bindingRepository.deleteByTeamIdAndEquipmentIds(request.getTeamId(), removedEquipmentIds);
            log.info("同步深度调整: 移除{}件超深度装备", removedEquipmentIds.size());
        }

        String affectedEquipmentsJson;
        try {
            affectedEquipmentsJson = objectMapper.writeValueAsString(removedEquipmentIds);
        } catch (JsonProcessingException e) {
            affectedEquipmentsJson = removedEquipmentIds.toString();
        }

        DepthAdjustRecord record = DepthAdjustRecord.builder()
                .teamId(request.getTeamId())
                .oldMaxDepth(oldMaxDepth)
                .newMaxDepth(newMaxDepth)
                .adjustType("DEPTH_REDUCTION")
                .affectedEquipments(affectedEquipmentsJson)
                .removedCount(removedEquipmentIds.size())
                .operator(request.getOperator() != null ? request.getOperator() : "system")
                .build();

        depthAdjustRecordRepository.save(record);

        if (!overDepthEquipments.isEmpty()) {
            notificationService.notifyDepthUnbind(team, newMaxDepth, overDepthEquipments);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("removedCount", removedEquipmentIds.size());
        result.put("removedEquipments", removedEquipmentIds);
        return result;
    }
}