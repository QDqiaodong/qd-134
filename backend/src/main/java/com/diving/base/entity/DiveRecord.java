package com.diving.base.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 下潜记录。同一小组任意时刻至多存在一条未收潜（OPEN）记录：
 * open 时把小组 id 冗余写入 {@link #activeTeamId}，收潜后置空，
 * 配合唯一索引 uk_active_team 在数据库层兜底，保证两人同时开潜也只会落下一条。
 */
@Entity
@Table(name = "dive_record", indexes = {
    @Index(name = "idx_dive_team_id", columnList = "team_id"),
    @Index(name = "idx_dive_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_active_team", columnNames = {"active_team_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiveRecord {

    /** 未收潜：小组正在水下，未收潜前不能再开第二条 */
    public static final String STATUS_OPEN = "OPEN";
    /** 已收潜：已填实际结束时刻，不可再改回未收潜 */
    public static final String STATUS_CLOSED = "CLOSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    /** 开潜（开始）时刻 */
    @Column(name = "start_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 预计结束时刻（开潜时填写） */
    @Column(name = "planned_end_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime plannedEndTime;

    /** 收潜（实际结束）时刻，收潜前为空 */
    @Column(name = "actual_end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualEndTime;

    /**
     * 状态：OPEN=未收潜，CLOSED=已收潜。
     * 列默认值保证历史数据迁移后不会出现空状态。
     */
    @Column(name = "status", columnDefinition = "varchar(20) not null default 'OPEN'")
    @Builder.Default
    private String status = STATUS_OPEN;

    /**
     * 未收潜互斥列：OPEN 时等于 teamId，CLOSED 时为 null。
     * MySQL 唯一索引允许多条 NULL，因此只有未收潜记录互相冲突。
     */
    @Column(name = "active_team_id")
    private Long activeTeamId;

    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
