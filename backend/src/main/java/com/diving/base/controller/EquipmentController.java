package com.diving.base.controller;

import com.diving.base.dto.request.EquipmentCreateRequest;
import com.diving.base.dto.response.ApiResponse;
import com.diving.base.dto.response.PageResponse;
import com.diving.base.entity.Equipment;
import com.diving.base.service.EquipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Equipment>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        PageResponse<Equipment> result = equipmentService.findAll(page, size, keyword);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Equipment>> getById(@PathVariable Long id) {
        Equipment equipment = equipmentService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(equipment));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<PageResponse<Equipment>>> filterByDepth(
            @RequestParam int minDepth,
            @RequestParam int maxDepth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<Equipment> result = equipmentService.findByDepthRange(minDepth, maxDepth, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Equipment>> create(@Valid @RequestBody EquipmentCreateRequest request) {
        Equipment equipment = equipmentService.create(request);
        return ResponseEntity.ok(ApiResponse.success("创建成功", equipment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Equipment>> update(
            @PathVariable Long id,
            @Valid @RequestBody EquipmentCreateRequest request) {
        Equipment equipment = equipmentService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", equipment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        equipmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}