package com.resumeai.enhancer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for an enhancement report.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancementReportResponse {

    private UUID id;
    private UUID resumeId;
    private UUID jdId;
    private BigDecimal overallMatchScore;
    private String skillGapAnalysis;
    private String enhancementSuggestions;
    private String keywordRecommendations;
    private String rewrittenSummary;
    private String rewrittenExperience;
    private LocalDateTime createdAt;
}
