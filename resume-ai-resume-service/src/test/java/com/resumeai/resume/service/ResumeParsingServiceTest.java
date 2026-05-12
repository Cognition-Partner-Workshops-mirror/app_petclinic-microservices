package com.resumeai.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.resume.model.Resume;
import com.resumeai.resume.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ResumeParsingService with mocked AI and repository.
 */
@ExtendWith(MockitoExtension.class)
class ResumeParsingServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private ChatClient chatClient;

    private ResumeParsingService service;

    @BeforeEach
    void setUp() {
        service = new ResumeParsingService(resumeRepository, chatClient, new ObjectMapper());
    }

    @Test
    void getById_existingResume_returnsResponse() {
        // Arrange
        UUID id = UUID.randomUUID();
        Resume resume = Resume.builder()
                .id(id)
                .candidateName("John Doe")
                .candidateEmail("john@example.com")
                .parsedSkills("[\"Java\", \"Spring\"]")
                .fileName("resume.pdf")
                .build();
        when(resumeRepository.findById(id)).thenReturn(Optional.of(resume));

        // Act
        var response = service.getById(id);

        // Assert
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getCandidateName()).isEqualTo("John Doe");
        assertThat(response.getCandidateEmail()).isEqualTo("john@example.com");
    }

    @Test
    void getById_nonExistentResume_throwsException() {
        UUID id = UUID.randomUUID();
        when(resumeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteById_existingResume_deletesSuccessfully() {
        UUID id = UUID.randomUUID();
        when(resumeRepository.existsById(id)).thenReturn(true);

        service.deleteById(id);

        verify(resumeRepository).deleteById(id);
    }

    @Test
    void deleteById_nonExistentResume_throwsException() {
        UUID id = UUID.randomUUID();
        when(resumeRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }
}
