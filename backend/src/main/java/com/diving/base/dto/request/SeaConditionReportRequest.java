package com.diving.base.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 海况单提交请求：日期 + 浪高 + 能见度 + 当天能否下水。
 * 同一天只能落下一张，重复提交由 Service 与唯一索引共同拦截。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeaConditionReportRequest {

    /** 海况单日期，不传时由后端按当天处理 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDate;

    @NotNull(message = "浪高不能为空")
    @DecimalMin(value = "0.0", message = "浪高不能为负数")
    private BigDecimal waveHeight;

    @NotNull(message = "能见度不能为空")
    @DecimalMin(value = "0.0", message = "能见度不能为负数")
    private BigDecimal visibility;

    /** 当天能否下水；不传默认可以下水 */
    private Boolean divable;

    /** 填报人（岸上人员） */
    private String reporter;

    private String remark;
}
