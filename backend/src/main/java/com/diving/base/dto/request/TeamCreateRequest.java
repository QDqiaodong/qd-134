package com.diving.base.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamCreateRequest {

    @NotBlank(message = "小组名称不能为空")
    private String name;

    private Integer memberCount;

    @Min(value = 0, message = "最小深度不能为负数")
    private Integer minDepth;

    @NotNull(message = "最大允许深度不能为空")
    @Positive(message = "最大允许深度必须为正数")
    @Max(value = 500, message = "最大允许深度不能超过500米")
    private Integer maxDepth;

    @NotNull(message = "持证最大深度不能为空")
    @Positive(message = "持证最大深度必须为正数")
    private Integer certifiedDepth;

    private Boolean depthChangeNotify;

    @Pattern(regexp = "FILMING|WRAPPED", message = "小组状态只能是 FILMING(拍摄中) 或 WRAPPED(已收队)")
    private String status;

    private String description;
}