package com.resumeai.enhancer.repository;

import com.resumeai.enhancer.model.EnhancementReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for EnhancementReport entities.
 */
@Repository
public interface EnhancementReportRepository extends JpaRepository<EnhancementReport, UUID> {

    /** Find all enhancement reports for a given resume */
    List<EnhancementReport> findByResumeId(UUID resumeId);
}
