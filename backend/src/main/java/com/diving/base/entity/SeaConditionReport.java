package com.diving.base.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日海况单。岸上人员每天填报一张：浪高、能见度、当天能否下水。
 *
 * 一天至多一张：{@link #reportDate} 上的唯一索引 uk_report_date 在数据库层兜底，
 * 两人同一天同时交单也只会落下一条。
 *
 * 只要某天的海况单记为不能下水（{@link #divable}=false），
 * 当天任何小组开潜都会被拦截，拦截提示需带出海况单上的浪高与能见度。
 */
@Entity
@Table(name = "sea_condition_report", uniqueConstraints = {
    @UniqueConstraint(name = "uk_report_date", columnNames = {"report_date"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeaConditionReport {

    /** 可以下水：当天开潜正常放行 */
    public static final boolean DIVABLE = true;
    /** 不能下水：当天开潜一律拦截 */
    public static final boolean NOT_DIVABLE = false;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 海况单所属日期（一天一张，唯一索引兜底） */
    @Column(name = "report_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDate;

    /** 浪高（米） */
    @Column(name = "wave_height", nullable = false, precision = 6, scale = 2)
    private BigDecimal waveHeight;

    /** 能见度（米） */
    @Column(name = "visibility", nullable = false, precision = 6, scale = 2)
    private BigDecimal visibility;

    /** 当天能否下水：true=可以，false=不能（不能下水时拦截全部开潜） */
    @Column(name = "divable", nullable = false, columnDefinition = "bit not null default 1")
    @Builder.Default
    private Boolean divable = DIVABLE;

    /** 填报人（岸上人员），未填时按默认值处理 */
    @Column(name = "reporter", length = 100)
    private String reporter;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

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
