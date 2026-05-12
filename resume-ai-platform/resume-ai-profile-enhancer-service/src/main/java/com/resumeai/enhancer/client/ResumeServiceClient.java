package com.resumeai.enhancer.client;

import com.resumeai.enhancer.dto.JobDescriptionResponse;
import com.resumeai.enhancer.dto.ResumeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client for calling resume-service via Eureka service discovery.
 * Fetches both resume data and job description data for enhancement analysis.
 */
@FeignClient(name = "resume-service")
public interface ResumeServiceClient {

    @GetMapping("/api/resumes/{id}")
    ResumeResponse getResume(@PathVariable("id") UUID id);

    @GetMapping("/api/jd/{id}")
    JobDescriptionResponse getJobDescription(@PathVariable("id") UUID id);
}
