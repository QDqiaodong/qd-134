package com.diving.base.dto.response;

import com.diving.base.entity.Team;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 小组视图，额外携带当前占用装备的登记重量合计与当前未收潜下潜记录。
 * totalWeight 只包含状态为 ACTIVE 的绑定装备，已解绑装备不计入；
 * 装备未填写重量时按 0 处理。
 * activeDive 是完整的未收潜互斥状态（开始/预计结束），不是小组旁的一个时间列。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {

    private Long id;
    private String name;
    private Integer memberCount;
    private Integer minDepth;
    private Integer maxDepth;
    private Integer certifiedDepth;
    private Boolean depthChangeNotify;
    private String status;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private BigDecimal totalWeight;

    /** 当前未收潜记录；小组没有在潜记录时为 null */
    private DiveRecordResponse activeDive;

    public static TeamResponse of(Team team, BigDecimal totalWeight) {
        return of(team, totalWeight, null);
    }

    public static TeamResponse of(Team team, BigDecimal totalWeight, DiveRecordResponse activeDive) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .memberCount(team.getMemberCount())
                .minDepth(team.getMinDepth())
                .maxDepth(team.getMaxDepth())
                .certifiedDepth(team.getCertifiedDepth())
                .depthChangeNotify(team.getDepthChangeNotify())
                .status(team.getStatus() != null ? team.getStatus() : Team.STATUS_FILMING)
                .description(team.getDescription())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .totalWeight(totalWeight != null ? totalWeight : BigDecimal.ZERO)
                .activeDive(activeDive)
                .build();
    }
}
