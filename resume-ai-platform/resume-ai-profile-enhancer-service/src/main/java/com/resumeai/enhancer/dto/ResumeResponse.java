package com.resumeai.enhancer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO mirroring the resume-service ResumeResponse for Feign deserialization.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeResponse {

    private UUID id;
    private String candidateName;
    private String candidateEmail;
    private String parsedSkills;
    private String parsedExperience;
    private String parsedEducation;
    private String parsedSummary;
    private String fileName;
    private String fileType;
    private LocalDateTime uploadedAt;
}
