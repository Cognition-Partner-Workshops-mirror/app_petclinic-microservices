package com.resumeai.enhancer.controller;

import com.resumeai.enhancer.dto.EnhancementReportResponse;
import com.resumeai.enhancer.service.ProfileEnhancerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for ProfileEnhancerController using MockMvc.
 */
@WebMvcTest(ProfileEnhancerController.class)
class ProfileEnhancerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileEnhancerService enhancerService;

    @Test
    void getReport_existingId_returns200() throws Exception {
        UUID reportId = UUID.randomUUID();
        EnhancementReportResponse response = EnhancementReportResponse.builder()
                .id(reportId)
                .resumeId(UUID.randomUUID())
                .overallMatchScore(BigDecimal.valueOf(85.5))
                .enhancementSuggestions("[\"Add more keywords\"]")
                .createdAt(LocalDateTime.now())
                .build();

        when(enhancerService.getReport(reportId)).thenReturn(response);

        mockMvc.perform(get("/api/enhance/reports/{reportId}", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallMatchScore").value(85.5));
    }
}
