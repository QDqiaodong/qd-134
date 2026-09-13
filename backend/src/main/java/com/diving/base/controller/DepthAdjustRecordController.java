package com.diving.base.controller;

import com.diving.base.dto.response.ApiResponse;
import com.diving.base.dto.response.PageResponse;
import com.diving.base.entity.DepthAdjustRecord;
import com.diving.base.repository.DepthAdjustRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/depth-adjust-record")
@RequiredArgsConstructor
public class DepthAdjustRecordController {

    private final DepthAdjustRecordRepository depthAdjustRecordRepository;

    @GetMapping
    public ApiResponse<PageResponse<DepthAdjustRecord>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "adjustedAt"));
        Page<DepthAdjustRecord> recordPage = depthAdjustRecordRepository.findAll(pageable);
        return ApiResponse.success(PageResponse.from(recordPage));
    }
}