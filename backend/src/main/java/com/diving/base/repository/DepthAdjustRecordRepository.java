package com.diving.base.repository;

import com.diving.base.entity.DepthAdjustRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepthAdjustRecordRepository extends JpaRepository<DepthAdjustRecord, Long> {

    List<DepthAdjustRecord> findByTeamIdOrderByAdjustedAtDesc(Long teamId);

    Page<DepthAdjustRecord> findByTeamId(Long teamId, Pageable pageable);
}