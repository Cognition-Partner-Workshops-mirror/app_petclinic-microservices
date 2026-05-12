package com.resumeai.interview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request body for interview question generation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRequest {

    @NotNull(message = "resumeId is required")
    private UUID resumeId;

    /** Difficulty level: EASY, MEDIUM, HARD, or MIXED */
    private String difficulty = "MIXED";

    @Min(1)
    @Max(50)
    private int numberOfQuestions = 10;
}
