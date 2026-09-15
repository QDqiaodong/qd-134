package com.diving.base.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 开下潜记录请求：开始时刻 + 预计结束时刻。
 * 预计结束必须晚于开始的跨字段校验在 Service 中完成（需给出统一的中文提示）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiveStartRequest {

    @NotNull(message = "小组ID不能为空")
    private Long teamId;

    @NotNull(message = "开始时刻不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "预计结束时刻不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime plannedEndTime;
}
