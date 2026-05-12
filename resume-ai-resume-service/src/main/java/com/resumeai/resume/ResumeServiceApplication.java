package com.resumeai.resume;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Resume Service — handles resume uploads, AI-powered parsing,
 * and job description management. Exposes REST APIs consumed
 * by the interview and profile-enhancer services via Feign.
 */
@SpringBootApplication
public class ResumeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeServiceApplication.class, args);
    }
}
