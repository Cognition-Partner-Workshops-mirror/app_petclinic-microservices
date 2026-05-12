package com.resumeai.enhancer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.enhancer.client.ResumeServiceClient;
import com.resumeai.enhancer.dto.EnhancementReportResponse;
import com.resumeai.enhancer.dto.ResumeResponse;
import com.resumeai.enhancer.model.EnhancementReport;
import com.resumeai.enhancer.repository.EnhancementReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ProfileEnhancerService with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class ProfileEnhancerServiceTest {

    @Mock
    private ResumeServiceClient resumeServiceClient;

    @Mock
    private ChatClient chatClient;

    @Mock
    private EnhancementReportRepository reportRepository;

    private ProfileEnhancerService service;

    @BeforeEach
    void setUp() {
        service = new ProfileEnhancerService(
                resumeServiceClient, chatClient, reportRepository, new ObjectMapper());
    }

    @Test
    void enhanceQuick_withValidResume_createsReport() {
        // Arrange
        UUID resumeId = UUID.randomUUID();
        ResumeResponse resume = ResumeResponse.builder()
                .id(resumeId)
                .parsedSkills("[\"Java\", \"AWS\"]")
                .parsedExperience("[\"3 years at TechCo\"]")
                .parsedEducation("[\"MS Computer Science\"]")
                .parsedSummary("Cloud engineer")
                .build();

        when(resumeServiceClient.getResume(resumeId)).thenReturn(resume);

        // Mock ChatClient chain
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("""
                {"overallMatchScore": 72, "skillGaps": [], "suggestions": ["Add certifications"], "keywordRecommendations": ["Kubernetes"], "rewrittenSummary": "Experienced cloud engineer...", "rewrittenExperience": []}
                """);

        // Mock save
        when(reportRepository.save(any(EnhancementReport.class)))
                .thenAnswer(invocation -> {
                    EnhancementReport report = invocation.getArgument(0);
                    report.setId(UUID.randomUUID());
                    return report;
                });

        // Act
        EnhancementReportResponse response = service.enhanceQuick(resumeId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getResumeId()).isEqualTo(resumeId);
        assertThat(response.getOverallMatchScore()).isNotNull();
    }

    @Test
    void getReport_nonExistent_throwsException() {
        UUID reportId = UUID.randomUUID();
        when(reportRepository.findById(reportId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.getReport(reportId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }
}
