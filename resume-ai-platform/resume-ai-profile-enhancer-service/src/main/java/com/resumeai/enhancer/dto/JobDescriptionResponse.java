package com.resumeai.enhancer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO mirroring the resume-service JobDescriptionResponse for Feign deserialization.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDescriptionResponse {

    private UUID id;
    private String title;
    private String company;
    private String description;
    private String requiredSkills;
    private String preferredSkills;
    private String experienceLevel;
    private LocalDateTime uploadedAt;
}
