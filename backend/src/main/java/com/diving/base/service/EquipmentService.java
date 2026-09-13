package com.diving.base.service;

import com.diving.base.dto.request.EquipmentCreateRequest;
import com.diving.base.dto.response.PageResponse;
import com.diving.base.entity.Equipment;
import com.diving.base.repository.EquipmentRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    @Cacheable(value = "equipment", key = "#id")
    public Equipment findById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("装备不存在: " + id));
    }

    @Cacheable(value = "equipmentList", key = "#page + '_' + #size + '_' + #keyword")
    public PageResponse<Equipment> findAll(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Equipment> equipmentPage;
        
        if (keyword != null && !keyword.isEmpty()) {
            equipmentPage = equipmentRepository.findByNameContainingOrCodeContaining(keyword, keyword, pageable);
        } else {
            equipmentPage = equipmentRepository.findAll(pageable);
        }
        
        return PageResponse.from(equipmentPage);
    }

    @Cacheable(value = "equipmentFilter", key = "#minDepth + '_' + #maxDepth + '_' + #page + '_' + #size")
    public PageResponse<Equipment> findByDepthRange(int minDepth, int maxDepth, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "maxDepth"));
        Page<Equipment> equipmentPage = equipmentRepository.findByDepthRange(minDepth, maxDepth, pageable);
        return PageResponse.from(equipmentPage);
    }

    @Transactional
    @CacheEvict(value = {"equipmentList", "equipmentFilter"}, allEntries = true)
    public Equipment create(EquipmentCreateRequest request) {
        if (equipmentRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("装备编号已存在: " + request.getCode());
        }

        Equipment equipment = Equipment.builder()
                .code(request.getCode())
                .name(request.getName())
                .maxDepth(request.getMaxDepth())
                .weight(request.getWeight())
                .specification(request.getSpecification())
                .description(request.getDescription())
                .build();

        return equipmentRepository.save(equipment);
    }

    @Transactional
    @CacheEvict(value = {"equipment", "equipmentList", "equipmentFilter"}, allEntries = true)
    public Equipment update(Long id, EquipmentCreateRequest request) {
        Equipment equipment = findById(id);

        if (!equipment.getCode().equals(request.getCode()) && equipmentRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("装备编号已存在: " + request.getCode());
        }

        equipment.setCode(request.getCode());
        equipment.setName(request.getName());
        equipment.setMaxDepth(request.getMaxDepth());
        equipment.setWeight(request.getWeight());
        equipment.setSpecification(request.getSpecification());
        equipment.setDescription(request.getDescription());

        return equipmentRepository.save(equipment);
    }

    @Transactional
    @CacheEvict(value = {"equipment", "equipmentList", "equipmentFilter"}, allEntries = true)
    public void delete(Long id) {
        if (!equipmentRepository.existsById(id)) {
            throw new RuntimeException("装备不存在: " + id);
        }
        equipmentRepository.deleteById(id);
    }
}