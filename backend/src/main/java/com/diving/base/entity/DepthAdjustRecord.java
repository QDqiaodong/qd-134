package com.diving.base.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "depth_adjust_record", indexes = {
    @Index(name = "idx_team_id", columnList = "team_id"),
    @Index(name = "idx_adjusted_at", columnList = "adjusted_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepthAdjustRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "old_max_depth", nullable = false)
    private Integer oldMaxDepth;

    @Column(name = "new_max_depth", nullable = false)
    private Integer newMaxDepth;

    @Column(name = "adjust_type", length = 50)
    private String adjustType;

    @Column(name = "affected_equipments", columnDefinition = "TEXT")
    private String affectedEquipments;

    @Column(name = "removed_count")
    @Builder.Default
    private Integer removedCount = 0;

    @Column(name = "operator", length = 100)
    private String operator;

    @Column(name = "adjusted_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime adjustedAt;

    @PrePersist
    protected void onCreate() {
        adjustedAt = LocalDateTime.now();
    }
}