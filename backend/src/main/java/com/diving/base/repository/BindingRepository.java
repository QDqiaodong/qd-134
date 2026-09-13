package com.diving.base.repository;

import com.diving.base.entity.Binding;
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

    @Modifying
    @Query("DELETE FROM Binding b WHERE b.teamId = :teamId")
    int deleteByTeamId(@Param("teamId") Long teamId);

    @Modifying
    @Query("DELETE FROM Binding b WHERE b.equipmentId = :equipmentId")
    int deleteByEquipmentId(@Param("equipmentId") Long equipmentId);
}