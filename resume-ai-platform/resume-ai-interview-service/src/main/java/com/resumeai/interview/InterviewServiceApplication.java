package com.resumeai.interview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Interview Service — generates AI-powered interview questions
 * based on parsed resume data fetched from resume-service via Feign.
 */
@SpringBootApplication
@EnableFeignClients
public class InterviewServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewServiceApplication.class, args);
    }
}
