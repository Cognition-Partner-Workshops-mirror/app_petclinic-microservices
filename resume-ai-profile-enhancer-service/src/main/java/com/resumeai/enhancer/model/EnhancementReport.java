package com.resumeai.enhancer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing an AI-generated profile enhancement report.
 * Contains match scores, gap analysis, suggestions, and rewritten sections.
 */
@Entity
@Table(name = "enhancement_reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancementReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "resume_id", nullable = false)
    private UUID resumeId;

    @Column(name = "jd_id")
    private UUID jdId;

    @Column(name = "overall_match_score", precision = 5, scale = 2)
    private BigDecimal overallMatchScore;

    @Column(name = "skill_gap_analysis", columnDefinition = "TEXT")
    private String skillGapAnalysis;

    @Column(name = "enhancement_suggestions", columnDefinition = "TEXT")
    private String enhancementSuggestions;

    @Column(name = "keyword_recommendations", columnDefinition = "TEXT")
    private String keywordRecommendations;

    @Column(name = "rewritten_summary", columnDefinition = "TEXT")
    private String rewrittenSummary;

    @Column(name = "rewritten_experience", columnDefinition = "TEXT")
    private String rewrittenExperience;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
