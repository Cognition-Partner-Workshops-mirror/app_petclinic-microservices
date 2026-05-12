package com.resumeai.enhancer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Profile Enhancer Service — compares resumes against job descriptions
 * using AI to provide gap analysis, keyword optimization, and rewriting suggestions.
 */
@SpringBootApplication
@EnableFeignClients
public class ProfileEnhancerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfileEnhancerServiceApplication.class, args);
    }
}
