package com.resumeai.resume.controller;

import com.resumeai.resume.dto.ResumeResponse;
import com.resumeai.resume.service.ResumeParsingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST endpoints for resume upload, retrieval, and deletion.
 */
@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeParsingService resumeParsingService;

    public ResumeController(ResumeParsingService resumeParsingService) {
        this.resumeParsingService = resumeParsingService;
    }

    /** Upload and parse a resume file (PDF, DOCX, etc.) */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponse> uploadResume(@RequestParam("file") MultipartFile file) {
        ResumeResponse response = resumeParsingService.parseAndStore(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Get a parsed resume by its UUID */
    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> getResume(@PathVariable UUID id) {
        return ResponseEntity.ok(resumeParsingService.getById(id));
    }

    /** List all resumes with pagination */
    @GetMapping
    public ResponseEntity<Page<ResumeResponse>> listResumes(Pageable pageable) {
        return ResponseEntity.ok(resumeParsingService.listAll(pageable));
    }

    /** Delete a resume by its UUID */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(@PathVariable UUID id) {
        resumeParsingService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
