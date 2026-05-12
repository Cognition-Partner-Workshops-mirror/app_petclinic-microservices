package com.resumeai.resume.repository;

import com.resumeai.resume.model.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for JobDescription entities.
 */
@Repository
public interface JobDescriptionRepository extends JpaRepository<JobDescription, UUID> {
}
