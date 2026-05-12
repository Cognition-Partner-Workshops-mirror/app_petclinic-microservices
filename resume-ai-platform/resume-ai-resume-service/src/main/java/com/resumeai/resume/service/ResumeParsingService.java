package com.resumeai.resume.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.resume.dto.ResumeResponse;
import com.resumeai.resume.exception.ResourceNotFoundException;
import com.resumeai.resume.model.Resume;
import com.resumeai.resume.repository.ResumeRepository;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Handles resume file uploads: extracts text via Apache Tika,
 * calls Groq AI to parse structured fields, and persists results.
 */
@Service
public class ResumeParsingService {

    private static final Logger LOG = LoggerFactory.getLogger(ResumeParsingService.class);

    private final ResumeRepository resumeRepository;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public ResumeParsingService(ResumeRepository resumeRepository,
                                ChatClient chatClient,
                                ObjectMapper objectMapper) {
        this.resumeRepository = resumeRepository;
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Parses an uploaded resume file: extracts raw text with Tika,
     * sends it to Groq AI for structured extraction, and saves to DB.
     */
    public ResumeResponse parseAndStore(MultipartFile file) {
        String rawText = extractText(file);

        // Build AI prompt to extract structured resume data
        String prompt = """
                Extract the following from this resume text in JSON format:
                {"candidateName": "...", "candidateEmail": "...", "skills": [...], "experience": [...], "education": [...], "summary": "..."}
                Only return valid JSON, no additional text.
                Resume text: %s
                """.formatted(rawText);

        LOG.info("Sending resume text to AI for structured extraction");
        String aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // Parse AI response and build entity
        Resume resume = buildResumeFromAiResponse(aiResponse, rawText, file);
        Resume saved = resumeRepository.save(resume);

        LOG.info("Resume saved with id={}", saved.getId());
        return toResponse(saved);
    }

    public ResumeResponse getById(UUID id) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + id));
        return toResponse(resume);
    }

    public Page<ResumeResponse> listAll(Pageable pageable) {
        return resumeRepository.findAll(pageable).map(this::toResponse);
    }

    public void deleteById(UUID id) {
        if (!resumeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resume not found with id: " + id);
        }
        resumeRepository.deleteById(id);
    }

    /**
     * Extracts plain text from an uploaded file using Apache Tika.
     */
    private String extractText(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(inputStream, handler, metadata, new ParseContext());
            return handler.toString();
        } catch (Exception e) {
            LOG.error("Failed to extract text from file: {}", file.getOriginalFilename(), e);
            throw new IllegalArgumentException("Unable to parse the uploaded file: " + e.getMessage());
        }
    }

    /**
     * Parses the AI JSON response and maps fields to a Resume entity.
     */
    private Resume buildResumeFromAiResponse(String aiResponse, String rawText, MultipartFile file) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            return Resume.builder()
                    .candidateName(root.path("candidateName").asText(null))
                    .candidateEmail(root.path("candidateEmail").asText(null))
                    .rawText(rawText)
                    .parsedSkills(root.path("skills").toString())
                    .parsedExperience(root.path("experience").toString())
                    .parsedEducation(root.path("education").toString())
                    .parsedSummary(root.path("summary").asText(null))
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .uploadedAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            LOG.warn("Failed to parse AI response as JSON, storing raw response", e);
            return Resume.builder()
                    .rawText(rawText)
                    .parsedSummary(aiResponse)
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .uploadedAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
    }

    private ResumeResponse toResponse(Resume resume) {
        return ResumeResponse.builder()
                .id(resume.getId())
                .candidateName(resume.getCandidateName())
                .candidateEmail(resume.getCandidateEmail())
                .parsedSkills(resume.getParsedSkills())
                .parsedExperience(resume.getParsedExperience())
                .parsedEducation(resume.getParsedEducation())
                .parsedSummary(resume.getParsedSummary())
                .fileName(resume.getFileName())
                .fileType(resume.getFileType())
                .uploadedAt(resume.getUploadedAt())
                .build();
    }
}
