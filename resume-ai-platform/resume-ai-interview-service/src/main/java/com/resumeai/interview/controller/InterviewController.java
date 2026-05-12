package com.resumeai.interview.controller;

import com.resumeai.interview.dto.GenerateRequest;
import com.resumeai.interview.dto.InterviewSessionResponse;
import com.resumeai.interview.service.InterviewQuestionGeneratorService;
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
 * REST endpoints for interview question generation and session retrieval.
 */
@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewQuestionGeneratorService generatorService;

    public InterviewController(InterviewQuestionGeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    /** Generate interview questions for a resume */
    @PostMapping("/generate")
    public ResponseEntity<InterviewSessionResponse> generate(@Valid @RequestBody GenerateRequest request) {
        InterviewSessionResponse response = generatorService.generate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Get an interview session with all questions by session ID */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<InterviewSessionResponse> getSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(generatorService.getSession(sessionId));
    }

    /** List all interview sessions for a given resume */
    @GetMapping("/sessions")
    public ResponseEntity<List<InterviewSessionResponse>> listSessions(@RequestParam UUID resumeId) {
        return ResponseEntity.ok(generatorService.getSessionsByResume(resumeId));
    }
}
