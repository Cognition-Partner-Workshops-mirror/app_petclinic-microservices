package com.resumeai.enhancer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request body for profile enhancement (with or without a job description).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnhanceRequest {

    @NotNull(message = "resumeId is required")
    private UUID resumeId;

    /** Optional job description ID for targeted enhancement */
    private UUID jdId;
}
