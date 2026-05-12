package com.resumeai.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO returned by job description REST endpoints.
 * Shared with the profile-enhancer service via Feign.
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
