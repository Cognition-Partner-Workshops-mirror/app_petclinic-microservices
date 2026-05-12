-- Initial schema for resume-service: resumes and job_descriptions tables

CREATE TABLE resumes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_name VARCHAR(255),
    candidate_email VARCHAR(255),
    raw_text TEXT,
    parsed_skills TEXT,
    parsed_experience TEXT,
    parsed_education TEXT,
    parsed_summary TEXT,
    file_name VARCHAR(255),
    file_type VARCHAR(50),
    uploaded_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE job_descriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    company VARCHAR(255),
    description TEXT NOT NULL,
    required_skills TEXT,
    preferred_skills TEXT,
    experience_level VARCHAR(50),
    uploaded_at TIMESTAMP DEFAULT NOW()
);
