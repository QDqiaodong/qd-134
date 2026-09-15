package com.diving.base.repository;

import com.diving.base.entity.SeaConditionReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface SeaConditionReportRepository extends JpaRepository<SeaConditionReport, Long> {

    /** 查某天的海况单；当天未交单时为空 */
    Optional<SeaConditionReport> findByReportDate(LocalDate reportDate);

    boolean existsByReportDate(LocalDate reportDate);

    Page<SeaConditionReport> findAllByOrderByReportDateDesc(Pageable pageable);
}
