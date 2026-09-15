package com.diving.base.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 下潜记录视图，联表带上小组名称。
 * 未收潜标记与超时标记均按记录时间实时计算，不落库。
 */
@Getter
@NoArgsConstructor
public class DiveRecordResponse {

    private Long id;
    private Long teamId;
    private String teamName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime plannedEndTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualEndTime;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public DiveRecordResponse(Long id, Long teamId, String teamName,
                              LocalDateTime startTime, LocalDateTime plannedEndTime,
                              LocalDateTime actualEndTime, String status,
                              LocalDateTime createdAt) {
        this.id = id;
        this.teamId = teamId;
        this.teamName = teamName;
        this.startTime = startTime;
        this.plannedEndTime = plannedEndTime;
        this.actualEndTime = actualEndTime;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** 未收潜：没有实际结束时刻 */
    @JsonProperty("open")
    public boolean isOpen() {
        return "OPEN".equals(status);
    }

    /**
     * 已超时：仍未收潜且当前时间已超过预计结束时刻；
     * 已收潜或尚未到预计结束时刻都不算超时。
     */
    @JsonProperty("overdue")
    public boolean isOverdue() {
        return isOpen()
                && plannedEndTime != null
                && LocalDateTime.now().isAfter(plannedEndTime);
    }
}
