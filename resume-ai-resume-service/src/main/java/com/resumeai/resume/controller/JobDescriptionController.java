package com.resumeai.resume.controller;

import com.resumeai.resume.dto.JobDescriptionResponse;
import com.resumeai.resume.service.JobDescriptionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * REST endpoints for job description upload and retrieval.
 * Accepts JSON body with JD fields.
 */
@RestController
@RequestMapping("/api/jd")
public class JobDescriptionController {

    private final JobDescriptionService jobDescriptionService;

    public JobDescriptionController(JobDescriptionService jobDescriptionService) {
        this.jobDescriptionService = jobDescriptionService;
    }

    /** Upload a new job description as JSON */
    @PostMapping("/upload")
    public ResponseEntity<JobDescriptionResponse> uploadJobDescription(@RequestBody Map<String, String> body) {
        JobDescriptionResponse response = jobDescriptionService.create(
                body.get("title"),
                body.get("company"),
                body.get("description"),
                body.get("requiredSkills"),
                body.get("preferredSkills"),
                body.get("experienceLevel")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Get a job description by UUID */
    @GetMapping("/{id}")
    public ResponseEntity<JobDescriptionResponse> getJobDescription(@PathVariable UUID id) {
        return ResponseEntity.ok(jobDescriptionService.getById(id));
    }

    /** List all job descriptions with pagination */
    @GetMapping
    public ResponseEntity<Page<JobDescriptionResponse>> listJobDescriptions(Pageable pageable) {
        return ResponseEntity.ok(jobDescriptionService.listAll(pageable));
    }
}
