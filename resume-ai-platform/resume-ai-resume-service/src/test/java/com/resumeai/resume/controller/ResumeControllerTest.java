package com.resumeai.resume.controller;

import com.resumeai.resume.dto.ResumeResponse;
import com.resumeai.resume.service.ResumeParsingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for ResumeController using MockMvc.
 * AI and database dependencies are mocked.
 */
@WebMvcTest(ResumeController.class)
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeParsingService resumeParsingService;

    @Test
    void getResume_existingId_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        ResumeResponse response = ResumeResponse.builder()
                .id(id)
                .candidateName("Jane Doe")
                .candidateEmail("jane@example.com")
                .parsedSkills("[\"Python\", \"AWS\"]")
                .fileName("resume.pdf")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(resumeParsingService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/api/resumes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateName").value("Jane Doe"))
                .andExpect(jsonPath("$.candidateEmail").value("jane@example.com"));
    }
}
