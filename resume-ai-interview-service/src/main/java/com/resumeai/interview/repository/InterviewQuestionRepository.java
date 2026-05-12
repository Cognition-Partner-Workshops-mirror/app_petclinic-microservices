package com.resumeai.interview.repository;

import com.resumeai.interview.model.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for InterviewQuestion entities.
 */
@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, UUID> {
}
