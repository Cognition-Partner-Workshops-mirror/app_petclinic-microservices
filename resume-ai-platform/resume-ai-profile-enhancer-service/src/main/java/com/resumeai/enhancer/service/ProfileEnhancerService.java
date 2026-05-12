package com.resumeai.enhancer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.enhancer.client.ResumeServiceClient;
import com.resumeai.enhancer.dto.EnhancementReportResponse;
import com.resumeai.enhancer.dto.JobDescriptionResponse;
import com.resumeai.enhancer.dto.ResumeResponse;
import com.resumeai.enhancer.model.EnhancementReport;
import com.resumeai.enhancer.repository.EnhancementReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Compares resumes against job descriptions using Groq AI.
 * Provides match scoring, gap analysis, keyword optimization, and rewriting.
 */
@Service
public class ProfileEnhancerService {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileEnhancerService.class);

    private final ResumeServiceClient resumeServiceClient;
    private final ChatClient chatClient;
    private final EnhancementReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    public ProfileEnhancerService(ResumeServiceClient resumeServiceClient,
                                  ChatClient chatClient,
                                  EnhancementReportRepository reportRepository,
                                  ObjectMapper objectMapper) {
        this.resumeServiceClient = resumeServiceClient;
        this.chatClient = chatClient;
        this.reportRepository = reportRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Enhances a resume against a specific job description.
     * Provides match score, gap analysis, suggestions, keyword optimization, and rewrites.
     */
    public EnhancementReportResponse enhanceWithJd(UUID resumeId, UUID jdId) {
        ResumeResponse resume = resumeServiceClient.getResume(resumeId);
        JobDescriptionResponse jd = resumeServiceClient.getJobDescription(jdId);

        String prompt = """
                Compare this resume against the job description. Provide JSON response:
                {"overallMatchScore": 0-100, "skillGaps": [...], "suggestions": [...], "keywordRecommendations": [...], "rewrittenSummary": "...", "rewrittenExperience": [...]}
                Only return valid JSON, no additional text.

                Resume:
                Skills: %s
                Experience: %s
                Education: %s
                Summary: %s

                Job Description:
                Title: %s
                Company: %s
                Description: %s
                Required Skills: %s
                Preferred Skills: %s
                Experience Level: %s
                """.formatted(
                resume.getParsedSkills(), resume.getParsedExperience(),
                resume.getParsedEducation(), resume.getParsedSummary(),
                jd.getTitle(), jd.getCompany(), jd.getDescription(),
                jd.getRequiredSkills(), jd.getPreferredSkills(), jd.getExperienceLevel()
        );

        LOG.info("Enhancing resume={} against jd={}", resumeId, jdId);
        String aiResponse = chatClient.prompt().user(prompt).call().content();

        EnhancementReport report = parseAndBuildReport(aiResponse, resumeId, jdId);
        EnhancementReport saved = reportRepository.save(report);
        LOG.info("Enhancement report saved with id={}", saved.getId());
        return toResponse(saved);
    }

    /**
     * Quick enhancement without a job description — general improvement suggestions.
     */
    public EnhancementReportResponse enhanceQuick(UUID resumeId) {
        ResumeResponse resume = resumeServiceClient.getResume(resumeId);

        String prompt = """
                Review this resume and provide general improvement suggestions in JSON format:
                {"overallMatchScore": 0-100, "skillGaps": [], "suggestions": [...], "keywordRecommendations": [...], "rewrittenSummary": "...", "rewrittenExperience": [...]}
                Score represents overall resume quality. Only return valid JSON, no additional text.

                Resume:
                Skills: %s
                Experience: %s
                Education: %s
                Summary: %s
                """.formatted(
                resume.getParsedSkills(), resume.getParsedExperience(),
                resume.getParsedEducation(), resume.getParsedSummary()
        );

        LOG.info("Quick enhancement for resume={}", resumeId);
        String aiResponse = chatClient.prompt().user(prompt).call().content();

        EnhancementReport report = parseAndBuildReport(aiResponse, resumeId, null);
        EnhancementReport saved = reportRepository.save(report);
        LOG.info("Quick enhancement report saved with id={}", saved.getId());
        return toResponse(saved);
    }

    public EnhancementReportResponse getReport(UUID reportId) {
        EnhancementReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Enhancement report not found: " + reportId));
        return toResponse(report);
    }

    public List<EnhancementReportResponse> getReportsByResume(UUID resumeId) {
        return reportRepository.findByResumeId(resumeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Parses the AI JSON response and builds an EnhancementReport entity.
     */
    private EnhancementReport parseAndBuildReport(String aiResponse, UUID resumeId, UUID jdId) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            return EnhancementReport.builder()
                    .resumeId(resumeId)
                    .jdId(jdId)
                    .overallMatchScore(BigDecimal.valueOf(root.path("overallMatchScore").asDouble(0)))
                    .skillGapAnalysis(root.path("skillGaps").toString())
                    .enhancementSuggestions(root.path("suggestions").toString())
                    .keywordRecommendations(root.path("keywordRecommendations").toString())
                    .rewrittenSummary(root.path("rewrittenSummary").asText(null))
                    .rewrittenExperience(root.path("rewrittenExperience").toString())
                    .createdAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            LOG.warn("Failed to parse AI response as JSON, storing raw", e);
            return EnhancementReport.builder()
                    .resumeId(resumeId)
                    .jdId(jdId)
                    .enhancementSuggestions(aiResponse)
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }

    private EnhancementReportResponse toResponse(EnhancementReport report) {
        return EnhancementReportResponse.builder()
                .id(report.getId())
                .resumeId(report.getResumeId())
                .jdId(report.getJdId())
                .overallMatchScore(report.getOverallMatchScore())
                .skillGapAnalysis(report.getSkillGapAnalysis())
                .enhancementSuggestions(report.getEnhancementSuggestions())
                .keywordRecommendations(report.getKeywordRecommendations())
                .rewrittenSummary(report.getRewrittenSummary())
                .rewrittenExperience(report.getRewrittenExperience())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
