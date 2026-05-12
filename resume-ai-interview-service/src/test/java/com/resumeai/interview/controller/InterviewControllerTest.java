package com.resumeai.interview.controller;

import com.resumeai.interview.dto.InterviewSessionResponse;
import com.resumeai.interview.service.InterviewQuestionGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for InterviewController using MockMvc.
 */
@WebMvcTest(InterviewController.class)
class InterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InterviewQuestionGeneratorService generatorService;

    @Test
    void getSession_existingId_returns200() throws Exception {
        UUID sessionId = UUID.randomUUID();
        InterviewSessionResponse response = InterviewSessionResponse.builder()
                .id(sessionId)
                .resumeId(UUID.randomUUID())
                .difficultyLevel("MIXED")
                .totalQuestions(5)
                .createdAt(LocalDateTime.now())
                .questions(List.of())
                .build();

        when(generatorService.getSession(sessionId)).thenReturn(response);

        mockMvc.perform(get("/api/interviews/sessions/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficultyLevel").value("MIXED"))
                .andExpect(jsonPath("$.totalQuestions").value(5));
    }
}
