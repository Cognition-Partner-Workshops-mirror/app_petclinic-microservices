package com.resumeai.resume.repository;

import com.resumeai.resume.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for Resume entities.
 */
@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {
}
