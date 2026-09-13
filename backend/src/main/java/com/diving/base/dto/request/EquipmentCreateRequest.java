package com.diving.base.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentCreateRequest {

    @NotBlank(message = "装备编号不能为空")
    private String code;

    @NotBlank(message = "装备名称不能为空")
    private String name;

    @NotNull(message = "最大耐压深度不能为空")
    @Positive(message = "最大耐压深度必须为正数")
    private Integer maxDepth;

    private BigDecimal weight;

    private String specification;

    private String description;
}