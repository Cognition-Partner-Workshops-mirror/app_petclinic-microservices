package com.resumeai.interview.repository;

import com.resumeai.interview.model.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for InterviewSession entities.
 */
@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {

    /** Find all sessions for a given resume */
    List<InterviewSession> findByResumeId(UUID resumeId);
}
