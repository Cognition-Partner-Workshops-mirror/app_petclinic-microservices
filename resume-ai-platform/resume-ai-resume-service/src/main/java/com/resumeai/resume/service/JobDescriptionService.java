package com.resumeai.resume.service;

import com.resumeai.resume.dto.JobDescriptionResponse;
import com.resumeai.resume.exception.ResourceNotFoundException;
import com.resumeai.resume.model.JobDescription;
import com.resumeai.resume.repository.JobDescriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Manages job description storage and retrieval.
 * Supports both plain-text JSON body and file uploads.
 */
@Service
public class JobDescriptionService {

    private static final Logger LOG = LoggerFactory.getLogger(JobDescriptionService.class);

    private final JobDescriptionRepository repository;

    public JobDescriptionService(JobDescriptionRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates a new job description from provided fields.
     */
    public JobDescriptionResponse create(String title, String company, String description,
                                         String requiredSkills, String preferredSkills,
                                         String experienceLevel) {
        JobDescription jd = JobDescription.builder()
                .title(title)
                .company(company)
                .description(description)
                .requiredSkills(requiredSkills)
                .preferredSkills(preferredSkills)
                .experienceLevel(experienceLevel)
                .uploadedAt(LocalDateTime.now())
                .build();

        JobDescription saved = repository.save(jd);
        LOG.info("Job description saved with id={}", saved.getId());
        return toResponse(saved);
    }

    public JobDescriptionResponse getById(UUID id) {
        JobDescription jd = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job description not found with id: " + id));
        return toResponse(jd);
    }

    public Page<JobDescriptionResponse> listAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    private JobDescriptionResponse toResponse(JobDescription jd) {
        return JobDescriptionResponse.builder()
                .id(jd.getId())
                .title(jd.getTitle())
                .company(jd.getCompany())
                .description(jd.getDescription())
                .requiredSkills(jd.getRequiredSkills())
                .preferredSkills(jd.getPreferredSkills())
                .experienceLevel(jd.getExperienceLevel())
                .uploadedAt(jd.getUploadedAt())
                .build();
    }
}
