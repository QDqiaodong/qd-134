package com.diving.base.controller;

import com.diving.base.dto.request.DiveEndRequest;
import com.diving.base.dto.request.DiveStartRequest;
import com.diving.base.dto.response.ApiResponse;
import com.diving.base.dto.response.DiveRecordResponse;
import com.diving.base.dto.response.PageResponse;
import com.diving.base.service.DiveRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dive")
@RequiredArgsConstructor
public class DiveRecordController {

    private final DiveRecordService diveRecordService;

    /** 下潜记录列表，可按小组与状态（OPEN/CLOSED）过滤 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DiveRecordResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String status) {
        PageResponse<DiveRecordResponse> result = diveRecordService.findAll(page, size, teamId, status);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** 某小组当前未收潜记录，没有时 data 为 null */
    @GetMapping("/active/{teamId}")
    public ResponseEntity<ApiResponse<DiveRecordResponse>> activeByTeam(@PathVariable Long teamId) {
        return ResponseEntity.ok(ApiResponse.success(diveRecordService.findActiveByTeam(teamId)));
    }

    /** 开下潜记录：开始时刻 + 预计结束时刻；同小组有未收潜记录时拒绝 */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<DiveRecordResponse>> start(@Valid @RequestBody DiveStartRequest request) {
        DiveRecordResponse record = diveRecordService.start(request);
        return ResponseEntity.ok(ApiResponse.success("开潜成功", record));
    }

    /** 收潜：必须填实际结束时刻，且不能早于开始时刻 */
    @PutMapping("/{id}/end")
    public ResponseEntity<ApiResponse<DiveRecordResponse>> end(
            @PathVariable Long id,
            @Valid @RequestBody DiveEndRequest request) {
        DiveRecordResponse record = diveRecordService.end(id, request);
        return ResponseEntity.ok(ApiResponse.success("收潜成功", record));
    }
}
