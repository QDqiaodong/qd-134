package com.diving.base.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "binding", indexes = {
    @Index(name = "idx_team_id", columnList = "team_id"),
    @Index(name = "idx_equipment_id", columnList = "equipment_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_team_equipment", columnNames = {"team_id", "equipment_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Binding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "bound_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime boundAt;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @PrePersist
    protected void onCreate() {
        boundAt = LocalDateTime.now();
    }
}