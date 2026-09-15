package com.diving.base.dto.response;

import com.diving.base.entity.SeaConditionReport;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 海况单视图：浪高、能见度、能否下水原样透出，供岸上查看与开潜拦截提示使用。
 */
@Getter
@NoArgsConstructor
public class SeaConditionReportResponse {

    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDate;

    private BigDecimal waveHeight;
    private BigDecimal visibility;
    private Boolean divable;
    private String reporter;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public SeaConditionReportResponse(SeaConditionReport report) {
        this.id = report.getId();
        this.reportDate = report.getReportDate();
        this.waveHeight = report.getWaveHeight();
        this.visibility = report.getVisibility();
        this.divable = report.getDivable();
        this.reporter = report.getReporter();
        this.remark = report.getRemark();
        this.createdAt = report.getCreatedAt();
        this.updatedAt = report.getUpdatedAt();
    }
}
