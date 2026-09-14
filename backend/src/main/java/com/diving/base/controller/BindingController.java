package com.diving.base.controller;

import com.diving.base.dto.request.BindingCreateRequest;
import com.diving.base.dto.request.BindingSyncRequest;
import com.diving.base.dto.response.ApiResponse;
import com.diving.base.dto.response.BindingResponse;
import com.diving.base.dto.response.PageResponse;
import com.diving.base.entity.Binding;
import com.diving.base.entity.Equipment;
import com.diving.base.service.BindingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/binding")
@RequiredArgsConstructor
public class BindingController {

    private final BindingService bindingService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BindingResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean overCertified) {
        PageResponse<BindingResponse> result = bindingService.findAll(page, size, overCertified);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Binding>> getById(@PathVariable Long id) {
        Binding binding = bindingService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(binding));
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<ApiResponse<List<Equipment>>> getByTeamId(@PathVariable Long teamId) {
        List<Equipment> equipments = bindingService.findEquipmentsByTeamId(teamId);
        return ResponseEntity.ok(ApiResponse.success(equipments));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Binding>> create(@Valid @RequestBody BindingCreateRequest request) {
        Binding binding = bindingService.create(request);
        return ResponseEntity.ok(ApiResponse.success("绑定成功", binding));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bindingService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("解绑成功", null));
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncDepth(@Valid @RequestBody BindingSyncRequest request) {
        Map<String, Object> result = bindingService.syncDepth(request);
        int removedCount = (Integer) result.get("removedCount");
        String message = removedCount > 0 
                ? String.format("同步完成，移除%d件超深度装备", removedCount)
                : "同步完成，无装备需要移除";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
}