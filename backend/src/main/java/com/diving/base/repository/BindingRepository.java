package com.diving.base.repository;

import com.diving.base.dto.response.BindingResponse;
import com.diving.base.entity.Binding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BindingRepository extends JpaRepository<Binding, Long> {

    Optional<Binding> findByTeamIdAndEquipmentId(Long teamId, Long equipmentId);

    boolean existsByTeamIdAndEquipmentId(Long teamId, Long equipmentId);

    List<Binding> findByTeamId(Long teamId);

    List<Binding> findByTeamIdAndStatus(Long teamId, String status);

    @Query("SELECT b.equipmentId FROM Binding b WHERE b.teamId = :teamId AND b.status = 'ACTIVE'")
    List<Long> findEquipmentIdsByTeamId(@Param("teamId") Long teamId);

    @Modifying
    @Query("DELETE FROM Binding b WHERE b.teamId = :teamId AND b.equipmentId IN :equipmentIds")
    int deleteByTeamIdAndEquipmentIds(@Param("teamId") Long teamId, @Param("equipmentIds") List<Long> equipmentIds);

    List<Binding> findByEquipmentId(Long equipmentId);

    /**
     * 绑定记录联表分页查询，overCertified 为 null 时不过滤；
     * 超证判定实时基于小组当前持证深度与装备额定深度比较。
     */
    @Query(value = "SELECT new com.diving.base.dto.response.BindingResponse(" +
            "b.id, t.id, t.name, t.certifiedDepth, e.id, e.name, e.maxDepth, b.boundAt, b.status) " +
            "FROM Binding b, Team t, Equipment e " +
            "WHERE b.teamId = t.id AND b.equipmentId = e.id " +
            "AND (:overCertified IS NULL " +
            "     OR (:overCertified = true AND e.maxDepth > t.certifiedDepth) " +
            "     OR (:overCertified = false AND e.maxDepth <= t.certifiedDepth)) " +
            "ORDER BY b.boundAt DESC",
            countQuery = "SELECT COUNT(b) FROM Binding b, Team t, Equipment e " +
            "WHERE b.teamId = t.id AND b.equipmentId = e.id " +
            "AND (:overCertified IS NULL " +
            "     OR (:overCertified = true AND e.maxDepth > t.certifiedDepth) " +
            "     OR (:overCertified = false AND e.maxDepth <= t.certifiedDepth))")
    Page<BindingResponse> findBindingViews(@Param("overCertified") Boolean overCertified, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Binding b WHERE b.teamId = :teamId")
    int deleteByTeamId(@Param("teamId") Long teamId);

    @Modifying
    @Query("DELETE FROM Binding b WHERE b.equipmentId = :equipmentId")
    int deleteByEquipmentId(@Param("equipmentId") Long equipmentId);
}