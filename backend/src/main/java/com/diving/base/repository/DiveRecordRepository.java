package com.diving.base.repository;

import com.diving.base.dto.response.DiveRecordResponse;
import com.diving.base.entity.DiveRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiveRecordRepository extends JpaRepository<DiveRecord, Long> {

    /**
     * 查某小组当前未收潜记录。同一小组至多一条，由 uk_active_team 唯一索引保证。
     */
    Optional<DiveRecord> findByActiveTeamId(Long teamId);

    boolean existsByActiveTeamId(Long teamId);

    /**
     * 批量取多个小组的未收潜记录，供小组列表展示在潜状态。
     */
    List<DiveRecord> findByActiveTeamIdIn(List<Long> teamIds);

    /**
     * 下潜记录联表分页。teamId/status 为 null 时不过滤，按开潜时刻倒序。
     */
    @Query(value = "SELECT new com.diving.base.dto.response.DiveRecordResponse(" +
            "d.id, d.teamId, t.name, d.startTime, d.plannedEndTime, d.actualEndTime, " +
            "d.status, d.createdAt) " +
            "FROM DiveRecord d, Team t " +
            "WHERE d.teamId = t.id " +
            "AND (:teamId IS NULL OR d.teamId = :teamId) " +
            "AND (:status IS NULL OR d.status = :status) " +
            "ORDER BY d.startTime DESC",
            countQuery = "SELECT COUNT(d) FROM DiveRecord d " +
            "WHERE (:teamId IS NULL OR d.teamId = :teamId) " +
            "AND (:status IS NULL OR d.status = :status)")
    Page<DiveRecordResponse> findRecordViews(@Param("teamId") Long teamId,
                                             @Param("status") String status,
                                             Pageable pageable);
}
