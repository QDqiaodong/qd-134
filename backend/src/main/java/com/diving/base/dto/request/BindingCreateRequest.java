package com.diving.base.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindingCreateRequest {

    @NotNull(message = "小组ID不能为空")
    private Long teamId;

    @NotNull(message = "装备ID不能为空")
    private Long equipmentId;
}