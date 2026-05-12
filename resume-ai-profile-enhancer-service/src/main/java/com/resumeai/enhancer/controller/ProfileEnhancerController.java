package com.resumeai.enhancer.controller;

import com.resumeai.enhancer.dto.EnhanceRequest;
import com.resumeai.enhancer.dto.EnhancementReportResponse;
import com.resumeai.enhancer.service.ProfileEnhancerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for profile enhancement, gap analysis, and report retrieval.
 */
@RestController
@RequestMapping("/api/enhance")
public class ProfileEnhancerController {

    private final ProfileEnhancerService enhancerService;

    public ProfileEnhancerController(ProfileEnhancerService enhancerService) {
        this.enhancerService = enhancerService;
    }

    /** Enhance a resume against a specific job description */
    @PostMapping
    public ResponseEntity<EnhancementReportResponse> enhance(@Valid @RequestBody EnhanceRequest request) {
        EnhancementReportResponse response = enhancerService.enhanceWithJd(
                request.getResumeId(), request.getJdId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Quick enhancement without a job description — general improvements */
    @PostMapping("/quick")
    public ResponseEntity<EnhancementReportResponse> quickEnhance(@Valid @RequestBody EnhanceRequest request) {
        EnhancementReportResponse response = enhancerService.enhanceQuick(request.getResumeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Get an enhancement report by ID */
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<EnhancementReportResponse> getReport(@PathVariable UUID reportId) {
        return ResponseEntity.ok(enhancerService.getReport(reportId));
    }

    /** List all enhancement reports for a resume */
    @GetMapping("/reports")
    public ResponseEntity<List<EnhancementReportResponse>> listReports(@RequestParam UUID resumeId) {
        return ResponseEntity.ok(enhancerService.getReportsByResume(resumeId));
    }
}
