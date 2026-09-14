package com.diving.base.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 绑定记录视图。超证标记不落库，每次查询时按当前小组持证深度与装备额定深度实时计算，
 * 保证列表刷新后标记与最新证深保持一致。
 */
@Getter
@NoArgsConstructor
public class BindingResponse {

    private Long id;
    private Long teamId;
    private String teamName;
    private Integer teamCertifiedDepth;
    private Long equipmentId;
    private String equipmentName;
    private Integer equipmentMaxDepth;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime boundAt;

    private String status;

    public BindingResponse(Long id, Long teamId, String teamName, Integer teamCertifiedDepth,
                           Long equipmentId, String equipmentName, Integer equipmentMaxDepth,
                           LocalDateTime boundAt, String status) {
        this.id = id;
        this.teamId = teamId;
        this.teamName = teamName;
        this.teamCertifiedDepth = teamCertifiedDepth;
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.equipmentMaxDepth = equipmentMaxDepth;
        this.boundAt = boundAt;
        this.status = status;
    }

    @JsonProperty("overCertified")
    public boolean isOverCertified() {
        return equipmentMaxDepth != null && teamCertifiedDepth != null
                && equipmentMaxDepth > teamCertifiedDepth;
    }
}
