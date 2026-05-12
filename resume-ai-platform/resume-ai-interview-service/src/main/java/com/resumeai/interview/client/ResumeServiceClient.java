package com.resumeai.interview.client;

import com.resumeai.interview.dto.ResumeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client for calling resume-service via Eureka service discovery.
 * Fetches parsed resume data used to generate interview questions.
 */
@FeignClient(name = "resume-service")
public interface ResumeServiceClient {

    @GetMapping("/api/resumes/{id}")
    ResumeResponse getResume(@PathVariable("id") UUID id);
}
