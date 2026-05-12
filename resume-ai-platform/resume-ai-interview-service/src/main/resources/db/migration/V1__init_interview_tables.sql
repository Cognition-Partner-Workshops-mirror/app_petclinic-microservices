-- Initial schema for interview-service: sessions and questions tables

CREATE TABLE interview_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID NOT NULL,
    difficulty_level VARCHAR(20),
    total_questions INT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE interview_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID REFERENCES interview_sessions(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    expected_answer_hints TEXT,
    difficulty VARCHAR(20),
    category VARCHAR(100),
    skill_tag VARCHAR(255),
    sort_order INT
);
