package com.diving.base.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindingSyncRequest {

    @NotNull(message = "小组ID不能为空")
    private Long teamId;

    @NotNull(message = "新的最大深度不能为空")
    @Positive(message = "新的最大深度必须为正数")
    private Integer newMaxDepth;

    private String operator;
}