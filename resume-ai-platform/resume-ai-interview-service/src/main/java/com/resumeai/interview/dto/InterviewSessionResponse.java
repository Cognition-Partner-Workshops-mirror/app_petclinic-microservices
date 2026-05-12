package com.resumeai.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for an interview session including its questions.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSessionResponse {

    private UUID id;
    private UUID resumeId;
    private String difficultyLevel;
    private Integer totalQuestions;
    private LocalDateTime createdAt;
    private List<QuestionResponse> questions;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResponse {
        private UUID id;
        private String questionText;
        private String expectedAnswerHints;
        private String difficulty;
        private String category;
        private String skillTag;
        private Integer sortOrder;
    }
}
