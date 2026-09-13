package com.diving.base.repository;

import com.diving.base.entity.Equipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Optional<Equipment> findByCode(String code);

    boolean existsByCode(String code);

    Page<Equipment> findByNameContainingOrCodeContaining(String name, String code, Pageable pageable);

    @Query("SELECT e FROM Equipment e WHERE e.maxDepth >= :minDepth AND e.maxDepth <= :maxDepth")
    Page<Equipment> findByDepthRange(@Param("minDepth") Integer minDepth, @Param("maxDepth") Integer maxDepth, Pageable pageable);

    @Query("SELECT e FROM Equipment e WHERE e.id IN :equipmentIds AND e.maxDepth > :maxDepth")
    List<Equipment> findByIdsAndMaxDepthGreaterThan(@Param("equipmentIds") List<Long> equipmentIds, @Param("maxDepth") Integer maxDepth);
}