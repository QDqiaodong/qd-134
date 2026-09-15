package com.diving.base.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "team", indexes = {
    @Index(name = "idx_max_depth", columnList = "max_depth")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    /** 拍摄中：正常剧组，可绑定装备 */
    public static final String STATUS_FILMING = "FILMING";
    /** 已收队：失效剧组，禁止再绑定装备 */
    public static final String STATUS_WRAPPED = "WRAPPED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "member_count")
    @Builder.Default
    private Integer memberCount = 0;

    @Column(name = "min_depth")
    @Builder.Default
    private Integer minDepth = 0;

    @Column(name = "max_depth", nullable = false)
    private Integer maxDepth;

    @Column(name = "certified_depth", nullable = false)
    private Integer certifiedDepth;

    @Column(name = "depth_change_notify", nullable = false)
    @Builder.Default
    private Boolean depthChangeNotify = true;

    /**
     * 剧组状态：FILMING=拍摄中，WRAPPED=已收队（失效剧组）。
     * 已收队的小组禁止再绑定装备，需管理员改回拍摄中后才可继续挂占用。
     * 列默认值保证存量小组迁移后仍为拍摄中。
     */
    @Column(name = "status", columnDefinition = "varchar(20) not null default 'FILMING'")
    @Builder.Default
    private String status = STATUS_FILMING;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

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