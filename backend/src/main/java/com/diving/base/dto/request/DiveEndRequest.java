package com.diving.base.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收潜请求：必须填写实际结束时刻。
 * 实际结束不能早于开始的跨记录校验在 Service 中完成。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiveEndRequest {

    @NotNull(message = "实际结束时刻不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualEndTime;
}
