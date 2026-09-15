package com.diving.base.controller;

import com.diving.base.dto.request.SeaConditionReportRequest;
import com.diving.base.dto.response.ApiResponse;
import com.diving.base.dto.response.PageResponse;
import com.diving.base.dto.response.SeaConditionReportResponse;
import com.diving.base.service.SeaConditionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 每日海况单：岸上交单（每天一张）、按日期查询、历史列表。
 */
@RestController
@RequestMapping("/api/sea-condition")
@RequiredArgsConstructor
public class SeaConditionController {

    private final SeaConditionService seaConditionService;

    /** 海况单历史列表，按日期倒序 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SeaConditionReportResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(seaConditionService.findAll(page, size)));
    }

    /** 查某天的海况单；当天未交单时 data 为 null */
    @GetMapping("/by-date")
    public ResponseEntity<ApiResponse<SeaConditionReportResponse>> byDate(
            @RequestParam(required = false) String date) {
        LocalDate reportDate = date != null && !date.isBlank() ? LocalDate.parse(date) : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(seaConditionService.findByDate(reportDate)));
    }

    /** 今天的海况单，未交单时 data 为 null */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<SeaConditionReportResponse>> today() {
        return ResponseEntity.ok(ApiResponse.success(seaConditionService.findByDate(LocalDate.now())));
    }

    /**
     * 交海况单：浪高、能见度、能否下水。同一天只能落一张，
     * 后到的提交会被拒绝并带回当天已记的浪高、能见度。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SeaConditionReportResponse>> report(
            @Valid @RequestBody SeaConditionReportRequest request) {
        SeaConditionReportResponse report = seaConditionService.report(request);
        return ResponseEntity.ok(ApiResponse.success("海况单已登记", report));
    }
}
