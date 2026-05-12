package com.resumeai.interview.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.interview.client.ResumeServiceClient;
import com.resumeai.interview.dto.GenerateRequest;
import com.resumeai.interview.dto.InterviewSessionResponse;
import com.resumeai.interview.dto.ResumeResponse;
import com.resumeai.interview.model.InterviewQuestion;
import com.resumeai.interview.model.InterviewSession;
import com.resumeai.interview.repository.InterviewSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates interview questions using Groq AI based on parsed resume data.
 * Fetches resume via Feign, builds a prompt, parses AI response, and persists results.
 */
@Service
public class InterviewQuestionGeneratorService {

    private static final Logger LOG = LoggerFactory.getLogger(InterviewQuestionGeneratorService.class);

    private final ResumeServiceClient resumeServiceClient;
    private final ChatClient chatClient;
    private final InterviewSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    public InterviewQuestionGeneratorService(ResumeServiceClient resumeServiceClient,
                                             ChatClient chatClient,
                                             InterviewSessionRepository sessionRepository,
                                             ObjectMapper objectMapper) {
        this.resumeServiceClient = resumeServiceClient;
        this.chatClient = chatClient;
        this.sessionRepository = sessionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Generates interview questions for a given resume.
     * 1. Fetches resume from resume-service via Feign
     * 2. Builds AI prompt with candidate profile and parameters
     * 3. Parses the AI JSON response into question entities
     * 4. Persists the session and questions to PostgreSQL
     */
    @Transactional
    public InterviewSessionResponse generate(GenerateRequest request) {
        // Fetch parsed resume from resume-service
        ResumeResponse resume = resumeServiceClient.getResume(request.getResumeId());

        // Build the AI prompt
        String prompt = """
                Based on the following candidate profile, generate %d interview questions.
                Difficulty level: %s (EASY, MEDIUM, HARD, or MIXED).
                For each question, provide a JSON array:
                [{"questionText": "...", "expectedAnswerHints": "...", "difficulty": "EASY|MEDIUM|HARD", "category": "Technical|Behavioral|System Design", "skillTag": "..."}]
                Only return valid JSON array, no additional text.

                Candidate Profile:
                Skills: %s
                Experience: %s
                Education: %s
                Summary: %s
                """.formatted(
                request.getNumberOfQuestions(),
                request.getDifficulty(),
                resume.getParsedSkills(),
                resume.getParsedExperience(),
                resume.getParsedEducation(),
                resume.getParsedSummary()
        );

        LOG.info("Generating {} interview questions for resume={}", request.getNumberOfQuestions(), request.getResumeId());

        String aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // Parse AI response into question list
        List<InterviewQuestion> questions = parseQuestions(aiResponse);

        // Build and save the session
        InterviewSession session = InterviewSession.builder()
                .resumeId(request.getResumeId())
                .difficultyLevel(request.getDifficulty())
                .totalQuestions(questions.size())
                .build();

        // Link questions to session
        for (int i = 0; i < questions.size(); i++) {
            InterviewQuestion q = questions.get(i);
            q.setSession(session);
            q.setSortOrder(i + 1);
        }
        session.setQuestions(questions);

        InterviewSession saved = sessionRepository.save(session);
        LOG.info("Interview session saved with id={}, questions={}", saved.getId(), saved.getTotalQuestions());

        return toResponse(saved);
    }

    public InterviewSessionResponse getSession(UUID sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Interview session not found: " + sessionId));
        return toResponse(session);
    }

    public List<InterviewSessionResponse> getSessionsByResume(UUID resumeId) {
        return sessionRepository.findByResumeId(resumeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Parses the AI JSON array response into InterviewQuestion entities.
     */
    private List<InterviewQuestion> parseQuestions(String aiResponse) {
        try {
            List<Map<String, String>> questionMaps = objectMapper.readValue(
                    aiResponse, new TypeReference<>() {});

            List<InterviewQuestion> questions = new ArrayList<>();
            for (Map<String, String> qMap : questionMaps) {
                questions.add(InterviewQuestion.builder()
                        .questionText(qMap.getOrDefault("questionText", ""))
                        .expectedAnswerHints(qMap.getOrDefault("expectedAnswerHints", ""))
                        .difficulty(qMap.getOrDefault("difficulty", "MEDIUM"))
                        .category(qMap.getOrDefault("category", "Technical"))
                        .skillTag(qMap.getOrDefault("skillTag", ""))
                        .build());
            }
            return questions;
        } catch (Exception e) {
            LOG.warn("Failed to parse AI response as JSON array, creating single question", e);
            List<InterviewQuestion> fallback = new ArrayList<>();
            fallback.add(InterviewQuestion.builder()
                    .questionText(aiResponse)
                    .difficulty("MEDIUM")
                    .category("General")
                    .build());
            return fallback;
        }
    }

    private InterviewSessionResponse toResponse(InterviewSession session) {
        List<InterviewSessionResponse.QuestionResponse> questions = session.getQuestions()
                .stream()
                .map(q -> InterviewSessionResponse.QuestionResponse.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .expectedAnswerHints(q.getExpectedAnswerHints())
                        .difficulty(q.getDifficulty())
                        .category(q.getCategory())
                        .skillTag(q.getSkillTag())
                        .sortOrder(q.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        return InterviewSessionResponse.builder()
                .id(session.getId())
                .resumeId(session.getResumeId())
                .difficultyLevel(session.getDifficultyLevel())
                .totalQuestions(session.getTotalQuestions())
                .createdAt(session.getCreatedAt())
                .questions(questions)
                .build();
    }
}
