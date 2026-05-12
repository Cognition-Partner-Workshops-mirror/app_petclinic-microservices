package com.resumeai.interview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.interview.client.ResumeServiceClient;
import com.resumeai.interview.dto.GenerateRequest;
import com.resumeai.interview.dto.InterviewSessionResponse;
import com.resumeai.interview.dto.ResumeResponse;
import com.resumeai.interview.model.InterviewSession;
import com.resumeai.interview.repository.InterviewSessionRepository;
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
 * Unit tests for InterviewQuestionGeneratorService with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class InterviewQuestionGeneratorServiceTest {

    @Mock
    private ResumeServiceClient resumeServiceClient;

    @Mock
    private ChatClient chatClient;

    @Mock
    private InterviewSessionRepository sessionRepository;

    private InterviewQuestionGeneratorService service;

    @BeforeEach
    void setUp() {
        service = new InterviewQuestionGeneratorService(
                resumeServiceClient, chatClient, sessionRepository, new ObjectMapper());
    }

    @Test
    void generate_withValidResume_createsSession() {
        // Arrange
        UUID resumeId = UUID.randomUUID();
        ResumeResponse resume = ResumeResponse.builder()
                .id(resumeId)
                .parsedSkills("[\"Java\", \"Spring Boot\"]")
                .parsedExperience("[\"5 years at Acme Corp\"]")
                .parsedEducation("[\"BS Computer Science\"]")
                .parsedSummary("Senior Java developer")
                .build();

        when(resumeServiceClient.getResume(resumeId)).thenReturn(resume);

        // Mock ChatClient chain
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("""
                [{"questionText": "Explain dependency injection in Spring", "expectedAnswerHints": "IoC container, beans", "difficulty": "MEDIUM", "category": "Technical", "skillTag": "Spring"}]
                """);

        // Mock save
        when(sessionRepository.save(any(InterviewSession.class)))
                .thenAnswer(invocation -> {
                    InterviewSession session = invocation.getArgument(0);
                    session.setId(UUID.randomUUID());
                    return session;
                });

        // Act
        GenerateRequest request = new GenerateRequest(resumeId, "MIXED", 5);
        InterviewSessionResponse response = service.generate(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getResumeId()).isEqualTo(resumeId);
        assertThat(response.getQuestions()).hasSize(1);
        assertThat(response.getQuestions().get(0).getQuestionText())
                .contains("dependency injection");
    }

    @Test
    void getSession_nonExistent_throwsException() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.getSession(sessionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }
}
