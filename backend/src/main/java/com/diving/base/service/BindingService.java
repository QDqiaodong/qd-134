package com.diving.base.service;

import com.diving.base.dto.request.BindingCreateRequest;
import com.diving.base.dto.request.BindingSyncRequest;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Cacheable(value = "binding", key = "#id")
    public Binding findById(Long id) {
        return bindingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("绑定关系不存在: " + id));
    }

    public PageResponse<Binding> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "boundAt"));
        Page<Binding> bindingPage = bindingRepository.findAll(pageable);
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

    @Transactional
    @CacheEvict(value = {"binding", "bindingTeam"}, allEntries = true)
    public Binding create(BindingCreateRequest request) {
        Team team = teamService.findById(request.getTeamId());
        Equipment equipment = equipmentService.findById(request.getEquipmentId());

        if (bindingRepository.existsByTeamIdAndEquipmentId(request.getTeamId(), request.getEquipmentId())) {
            throw new RuntimeException("该装备已绑定到该小组");
        }

        depthValidationService.validateBinding(team, equipment);

        Binding binding = Binding.builder()
                .teamId(request.getTeamId())
                .equipmentId(request.getEquipmentId())
                .status("ACTIVE")
                .build();

        return bindingRepository.save(binding);
    }

    @Transactional
    @CacheEvict(value = {"binding", "bindingTeam"}, allEntries = true)
    public void delete(Long id) {
        Binding binding = findById(id);
        bindingRepository.delete(binding);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    @CacheEvict(value = {"binding", "bindingTeam"}, allEntries = true)
    public Map<String, Object> syncDepth(BindingSyncRequest request) {
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RuntimeException("小组不存在: " + request.getTeamId()));
        Integer oldMaxDepth = team.getMaxDepth();
        Integer newMaxDepth = request.getNewMaxDepth();

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