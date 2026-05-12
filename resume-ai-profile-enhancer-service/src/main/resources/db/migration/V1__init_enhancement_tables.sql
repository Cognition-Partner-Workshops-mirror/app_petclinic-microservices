-- Initial schema for profile-enhancer-service: enhancement reports table

CREATE TABLE enhancement_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID NOT NULL,
    jd_id UUID,
    overall_match_score DECIMAL(5,2),
    skill_gap_analysis TEXT,
    enhancement_suggestions TEXT,
    keyword_recommendations TEXT,
    rewritten_summary TEXT,
    rewritten_experience TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
