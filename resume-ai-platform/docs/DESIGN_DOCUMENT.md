# Resume AI Platform — Design Document

## 1. Overview

Resume AI Platform is a cloud-native microservices application that leverages large language models (LLMs) to automate resume processing workflows. It addresses two primary use cases:

1. **Interview Question Generation** — Upload a candidate's resume, extract structured data via AI, and generate tailored interview questions at configurable difficulty levels (Easy, Medium, Hard, or Mixed).
2. **Profile Enhancement** — Compare a candidate's resume against a job description (or perform a standalone quality review) to produce gap analysis, keyword optimization recommendations, and AI-rewritten resume sections.

The platform is built on **Spring Boot 3.2.5** with **Spring Cloud** for microservice orchestration and **Spring AI** for LLM integration. All AI features use the **Groq API** (which exposes an OpenAI-compatible interface), powered by the **Llama 3.3 70B Versatile** model.

---

## 2. Technology Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Language** | Java | 17+ | Primary development language |
| **Framework** | Spring Boot | 3.2.5 | Application framework |
| **Cloud** | Spring Cloud | 2023.0.1 | Microservice infrastructure (Config, Discovery, Gateway) |
| **AI** | Spring AI | 1.0.0-M6 | LLM integration via OpenAI-compatible API |
| **LLM Provider** | Groq (Llama 3.3 70B) | — | AI inference engine (OpenAI-API-compatible) |
| **Database** | PostgreSQL | 16 | Persistent storage for all services |
| **Migrations** | Flyway | (managed by Spring Boot BOM) | Version-controlled database schema management |
| **ORM** | Spring Data JPA / Hibernate | (managed by Spring Boot BOM) | Object-relational mapping |
| **Service Discovery** | Netflix Eureka | (via Spring Cloud) | Dynamic service registration and lookup |
| **Configuration** | Spring Cloud Config Server | (via Spring Cloud) | Centralized externalized configuration |
| **API Gateway** | Spring Cloud Gateway (WebFlux) | (via Spring Cloud) | Edge routing, load balancing, CORS |
| **Inter-Service Comm** | Spring Cloud OpenFeign | (via Spring Cloud) | Declarative HTTP clients between services |
| **Document Parsing** | Apache Tika | 2.9.1 | PDF and DOCX text extraction |
| **Build** | Maven (multi-module) | 3.8+ | Build automation |
| **Containerization** | Docker / Docker Compose | — | Multi-stage builds and orchestration |
| **Base Image** | Eclipse Temurin | 17 (JDK/JRE) | Lightweight Java runtime for containers |
| **Code Generation** | Lombok | (managed by Spring Boot BOM) | Boilerplate reduction (getters, setters, builders) |

---

## 3. Architecture

### 3.1 High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Client Applications                             │
│                    (curl, Postman, Frontend, etc.)                          │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
                                   ▼
                        ┌─────────────────────┐
                        │   API Gateway        │
                        │   (port 8080)        │
                        │   Spring Cloud       │
                        │   Gateway WebFlux    │
                        └──────────┬──────────┘
                                   │
                    ┌──────────────┼──────────────┐
                    │              │              │
                    ▼              ▼              ▼
          ┌─────────────┐ ┌──────────────┐ ┌──────────────────┐
          │  Resume      │ │  Interview   │ │ Profile Enhancer │
          │  Service     │ │  Service     │ │ Service          │
          │  (8081)      │ │  (8082)      │ │ (8083)           │
          └──────┬───────┘ └──────┬───────┘ └────────┬─────────┘
                 │                │  Feign           │  Feign
                 │                │───────►          │───────►
                 │                │  (resume-service) │  (resume-service)
                 │                │                   │
                 ▼                ▼                   ▼
          ┌─────────────────────────────────────────────────┐
          │               PostgreSQL 16                      │
          │               Database: resumeai                 │
          │  ┌──────────┐ ┌──────────────┐ ┌──────────────┐ │
          │  │ resumes  │ │ interview_   │ │ enhancement_ │ │
          │  │ job_desc │ │ sessions     │ │ reports      │ │
          │  │          │ │ questions    │ │              │ │
          │  └──────────┘ └──────────────┘ └──────────────┘ │
          └─────────────────────────────────────────────────┘

          ┌──────────────────┐    ┌──────────────────┐
          │  Config Server   │    │ Discovery Server │
          │  (8888)          │    │ (8761) — Eureka  │
          │  Spring Cloud    │    │                  │
          │  Config (native) │    │                  │
          └──────────────────┘    └──────────────────┘

          ┌──────────────────┐
          │    Groq API      │
          │  (external LLM)  │
          │  llama-3.3-70b   │
          └──────────────────┘
```

### 3.2 Communication Patterns

- **Client → Services**: All external traffic enters through the **API Gateway** on port 8080, which routes requests to downstream services using Eureka-resolved `lb://` URIs.
- **Service → Service**: The **Interview Service** and **Profile Enhancer Service** call the **Resume Service** via **OpenFeign** declarative HTTP clients. Feign uses Eureka service discovery to resolve `resume-service` to its actual host/port.
- **Service → AI**: Three services (Resume, Interview, Profile Enhancer) call the **Groq API** via Spring AI's `ChatClient`, which sends HTTP requests to `https://api.groq.com/openai`.
- **Service → Database**: Each data service connects to the shared **PostgreSQL** instance. Schema isolation is by table namespace — each service owns its own set of tables managed by Flyway.
- **Config Distribution**: All services fetch configuration from the **Config Server** at startup via `spring.config.import: configserver:http://localhost:8888`.

### 3.3 Startup Ordering (Docker Compose)

Services start in a strict dependency order enforced by Docker Compose `depends_on` with `service_healthy` conditions:

```
PostgreSQL ──► (healthcheck: pg_isready)
Config Server ──► (healthcheck: /actuator/health)
    └──► Discovery Server ──► (healthcheck: /actuator/health)
              └──► API Gateway
              └──► Resume Service (also depends on PostgreSQL)
              └──► Interview Service (also depends on PostgreSQL)
              └──► Profile Enhancer Service (also depends on PostgreSQL)
```

---

## 4. Service Descriptions

### 4.1 Config Server (`resume-ai-config-server`)

| Property | Value |
|----------|-------|
| **Port** | 8888 |
| **Purpose** | Centralized externalized configuration management |
| **Key Dependency** | `spring-cloud-config-server` |

Serves YAML configuration files to all services from `classpath:/config/` using the **native** profile (no Git repository required). Configuration files served:

| File | Scope |
|------|-------|
| `application.yml` | Shared Eureka client settings and Actuator endpoints |
| `resume-service.yml` | Datasource, file upload limits, AI config (temperature 0.3) |
| `interview-service.yml` | Datasource, AI config (temperature 0.7) |
| `profile-enhancer-service.yml` | Datasource, AI config (temperature 0.7) |

The lower temperature (0.3) for Resume Service ensures deterministic structured extraction, while the higher temperature (0.7) for Interview and Profile Enhancer Services produces more creative and varied outputs.

### 4.2 Discovery Server (`resume-ai-discovery-server`)

| Property | Value |
|----------|-------|
| **Port** | 8761 |
| **Purpose** | Service registration and discovery |
| **Key Dependency** | `spring-cloud-starter-netflix-eureka-server` |

Runs a Netflix Eureka Server that all services register with. Configured with `register-with-eureka: false` and `fetch-registry: false` since it is the registry itself. Provides a web dashboard at `http://localhost:8761` for viewing registered service instances.

### 4.3 API Gateway (`resume-ai-api-gateway`)

| Property | Value |
|----------|-------|
| **Port** | 8080 |
| **Purpose** | Edge routing and load balancing |
| **Key Dependency** | `spring-cloud-starter-gateway-server-webflux` |

Routes incoming requests to the appropriate downstream service based on path predicates:

| Route ID | Path Predicate | Target Service |
|----------|---------------|----------------|
| `resume-service` | `/api/resumes/**`, `/api/jd/**` | `lb://resume-service` |
| `interview-service` | `/api/interviews/**` | `lb://interview-service` |
| `profile-enhancer-service` | `/api/enhance/**` | `lb://profile-enhancer-service` |

Includes a global CORS configuration (`CorsWebFilter`) that permits all origins, methods, and headers for development use.

### 4.4 Resume Service (`resume-ai-resume-service`)

| Property | Value |
|----------|-------|
| **Port** | 8081 |
| **Purpose** | Resume upload, parsing, and job description management |
| **Key Dependencies** | Spring Web, Spring Data JPA, Apache Tika 2.9.1, Spring AI OpenAI, Flyway |

**Core Functionality:**
1. **File Upload & Text Extraction** — Accepts PDF, DOCX, and other document formats via multipart upload. Uses Apache Tika's `AutoDetectParser` to extract plain text from the binary file.
2. **AI-Powered Structured Extraction** — Sends extracted text to Groq AI with a prompt requesting JSON output containing `candidateName`, `candidateEmail`, `skills`, `experience`, `education`, and `summary`. Parses the response with Jackson and persists structured fields.
3. **Job Description Management** — Accepts job descriptions as JSON payloads with fields like title, company, required/preferred skills, and experience level.

**Error Handling:** Centralized `@RestControllerAdvice` (`GlobalExceptionHandler`) handles `ResourceNotFoundException` (404), `MaxUploadSizeExceededException` (413), `IllegalArgumentException` (400), and general exceptions (500) with a consistent JSON error format: `{timestamp, status, error, message, path}`.

### 4.5 Interview Service (`resume-ai-interview-service`)

| Property | Value |
|----------|-------|
| **Port** | 8082 |
| **Purpose** | AI-powered interview question generation |
| **Key Dependencies** | Spring Web, Spring Data JPA, OpenFeign, Spring AI OpenAI, Flyway |

**Core Functionality:**
1. **Resume Fetching** — Uses a `@FeignClient(name = "resume-service")` to call `GET /api/resumes/{id}` on the Resume Service and retrieve structured resume data.
2. **Prompt Construction** — Builds an AI prompt containing the candidate's skills, experience, education, and summary, along with the requested number of questions and difficulty level.
3. **Question Generation** — Calls Groq AI to generate a JSON array of questions, each with `questionText`, `expectedAnswerHints`, `difficulty` (EASY/MEDIUM/HARD), `category` (Technical/Behavioral/System Design), and `skillTag`.
4. **Persistence** — Creates an `InterviewSession` with linked `InterviewQuestion` entities (parent-child, cascade-all, orphan-removal) and persists within a `@Transactional` boundary.

### 4.6 Profile Enhancer Service (`resume-ai-profile-enhancer-service`)

| Property | Value |
|----------|-------|
| **Port** | 8083 |
| **Purpose** | Resume-to-JD comparison, gap analysis, and profile optimization |
| **Key Dependencies** | Spring Web, Spring Data JPA, OpenFeign, Spring AI OpenAI, Flyway |

**Core Functionality:**
1. **Data Fetching** — Uses Feign to fetch both the parsed resume and job description from the Resume Service.
2. **Full Enhancement (with JD)** — Compares resume against job description and requests AI-generated JSON with: `overallMatchScore` (0–100), `skillGaps`, `suggestions`, `keywordRecommendations`, `rewrittenSummary`, and `rewrittenExperience`.
3. **Quick Enhancement (without JD)** — Performs a standalone resume quality review without a job description, providing general improvement suggestions.
4. **Fallback Handling** — If AI response JSON parsing fails, stores the raw AI response text in the report fields to avoid data loss.

---

## 5. Data Model

### 5.1 Database Schema

All services share a single PostgreSQL database (`resumeai`) but manage independent table schemas via Flyway migrations.

#### `resumes` table (owned by Resume Service)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Unique identifier |
| `candidate_name` | VARCHAR(255) | — | AI-extracted candidate name |
| `candidate_email` | VARCHAR(255) | — | AI-extracted candidate email |
| `raw_text` | TEXT | — | Full text extracted by Apache Tika |
| `parsed_skills` | TEXT | — | JSON array of skills |
| `parsed_experience` | TEXT | — | JSON array of experience entries |
| `parsed_education` | TEXT | — | JSON array of education entries |
| `parsed_summary` | TEXT | — | AI-generated career summary |
| `file_name` | VARCHAR(255) | — | Original uploaded filename |
| `file_type` | VARCHAR(50) | — | MIME type of uploaded file |
| `uploaded_at` | TIMESTAMP | DEFAULT NOW() | Upload timestamp |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update timestamp |

#### `job_descriptions` table (owned by Resume Service)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Unique identifier |
| `title` | VARCHAR(255) | NOT NULL | Job title |
| `company` | VARCHAR(255) | — | Company name |
| `description` | TEXT | NOT NULL | Full job description text |
| `required_skills` | TEXT | — | Required skills (comma-separated or JSON) |
| `preferred_skills` | TEXT | — | Preferred/nice-to-have skills |
| `experience_level` | VARCHAR(50) | — | e.g., "Senior", "Mid-Level" |
| `uploaded_at` | TIMESTAMP | DEFAULT NOW() | Upload timestamp |

#### `interview_sessions` table (owned by Interview Service)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Unique identifier |
| `resume_id` | UUID | NOT NULL | Reference to the source resume |
| `difficulty_level` | VARCHAR(20) | — | EASY, MEDIUM, HARD, or MIXED |
| `total_questions` | INT | — | Number of generated questions |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Session creation timestamp |

#### `interview_questions` table (owned by Interview Service)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Unique identifier |
| `session_id` | UUID | FK → interview_sessions(id) ON DELETE CASCADE | Parent session |
| `question_text` | TEXT | NOT NULL | The interview question |
| `expected_answer_hints` | TEXT | — | Suggested answer guidelines |
| `difficulty` | VARCHAR(20) | — | EASY, MEDIUM, or HARD |
| `category` | VARCHAR(100) | — | Technical, Behavioral, or System Design |
| `skill_tag` | VARCHAR(255) | — | Relevant skill or technology |
| `sort_order` | INT | — | Display order within session |

#### `enhancement_reports` table (owned by Profile Enhancer Service)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Unique identifier |
| `resume_id` | UUID | NOT NULL | Reference to the source resume |
| `jd_id` | UUID | — (nullable for quick mode) | Reference to the job description |
| `overall_match_score` | DECIMAL(5,2) | — | AI-computed match score (0–100) |
| `skill_gap_analysis` | TEXT | — | JSON array of identified skill gaps |
| `enhancement_suggestions` | TEXT | — | JSON array of improvement suggestions |
| `keyword_recommendations` | TEXT | — | JSON array of keyword optimizations |
| `rewritten_summary` | TEXT | — | AI-rewritten professional summary |
| `rewritten_experience` | TEXT | — | AI-rewritten experience entries |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Report generation timestamp |

### 5.2 Entity Relationships

```
Resume (1) ──────────── (*) InterviewSession
  │                            │
  │                            └── (1) ──── (*) InterviewQuestion
  │
  └── (1) ──────────── (*) EnhancementReport
                              │
JobDescription (0..1) ────────┘
```

- A **Resume** can have many **InterviewSessions** and many **EnhancementReports** (linked via `resume_id`).
- Each **InterviewSession** contains many **InterviewQuestions** (JPA `@OneToMany` with cascade and orphan removal).
- An **EnhancementReport** optionally references a **JobDescription** (null `jd_id` for quick-mode reports).
- Cross-service references (`resume_id`, `jd_id`) are logical (UUID values) rather than database foreign keys, since tables are owned by different services.

---

## 6. API Surface

### 6.1 Resume Service Endpoints

| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| `POST` | `/api/resumes/upload` | Upload and parse a resume | Multipart file (`file`) | `201` — ResumeResponse |
| `GET` | `/api/resumes/{id}` | Get a parsed resume by ID | Path: UUID | `200` — ResumeResponse |
| `GET` | `/api/resumes` | List all resumes (paginated) | Query: `page`, `size`, `sort` | `200` — Page\<ResumeResponse\> |
| `DELETE` | `/api/resumes/{id}` | Delete a resume | Path: UUID | `204` — No Content |
| `POST` | `/api/jd/upload` | Create a job description | JSON body (title, company, description, requiredSkills, preferredSkills, experienceLevel) | `201` — JobDescriptionResponse |
| `GET` | `/api/jd/{id}` | Get a job description by ID | Path: UUID | `200` — JobDescriptionResponse |
| `GET` | `/api/jd` | List all job descriptions (paginated) | Query: `page`, `size`, `sort` | `200` — Page\<JobDescriptionResponse\> |

### 6.2 Interview Service Endpoints

| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| `POST` | `/api/interviews/generate` | Generate interview questions | JSON body: `{resumeId, difficulty, numberOfQuestions}` | `201` — InterviewSessionResponse |
| `GET` | `/api/interviews/sessions/{sessionId}` | Get a session with all questions | Path: UUID | `200` — InterviewSessionResponse |
| `GET` | `/api/interviews/sessions?resumeId={uuid}` | List sessions for a resume | Query: `resumeId` | `200` — List\<InterviewSessionResponse\> |

### 6.3 Profile Enhancer Service Endpoints

| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| `POST` | `/api/enhance` | Enhance resume against a JD | JSON body: `{resumeId, jdId}` | `201` — EnhancementReportResponse |
| `POST` | `/api/enhance/quick` | Quick enhancement (no JD) | JSON body: `{resumeId}` | `201` — EnhancementReportResponse |
| `GET` | `/api/enhance/reports/{reportId}` | Get an enhancement report | Path: UUID | `200` — EnhancementReportResponse |
| `GET` | `/api/enhance/reports?resumeId={uuid}` | List reports for a resume | Query: `resumeId` | `200` — List\<EnhancementReportResponse\> |

### 6.4 Error Response Format

All services return errors in a consistent JSON structure:

```json
{
  "timestamp": "2025-01-15T10:30:00.000",
  "status": 404,
  "error": "Not Found",
  "message": "Resume not found with id: 550e8400-e29b-41d4-a716-446655440000",
  "path": "/api/resumes/550e8400-e29b-41d4-a716-446655440000"
}
```

---

## 7. Feature Workflows

### 7.1 Resume Upload and Parsing

```
Client                  API Gateway         Resume Service         Groq AI
  │                         │                     │                    │
  │── POST /api/resumes/    │                     │                    │
  │   upload (file.pdf) ───►│── route to          │                    │
  │                         │   resume-service ──►│                    │
  │                         │                     │── Tika extract ──►│
  │                         │                     │   raw text         │
  │                         │                     │                    │
  │                         │                     │── ChatClient ─────►│
  │                         │                     │   "Extract JSON    │
  │                         │                     │    from resume..." │
  │                         │                     │◄── JSON response ──│
  │                         │                     │                    │
  │                         │                     │── parse JSON       │
  │                         │                     │── save to DB       │
  │◄── 201 ResumeResponse ──│◄────────────────────│                    │
```

### 7.2 Interview Question Generation

```
Client              API Gateway       Interview Service     Resume Service     Groq AI
  │                     │                    │                     │               │
  │── POST /api/        │                    │                     │               │
  │   interviews/       │                    │                     │               │
  │   generate ────────►│── route ──────────►│                     │               │
  │                     │                    │── Feign GET ────────►│               │
  │                     │                    │   /api/resumes/{id}  │               │
  │                     │                    │◄── ResumeResponse ──│               │
  │                     │                    │                     │               │
  │                     │                    │── ChatClient ───────────────────────►│
  │                     │                    │   "Generate N questions..."          │
  │                     │                    │◄── JSON array ──────────────────────│
  │                     │                    │                                      │
  │                     │                    │── parse questions                    │
  │                     │                    │── save session + questions           │
  │◄── 201 Session ─────│◄───────────────────│                                     │
```

### 7.3 Profile Enhancement (with Job Description)

```
Client              API Gateway       Profile Enhancer      Resume Service     Groq AI
  │                     │                    │                     │               │
  │── POST /api/        │                    │                     │               │
  │   enhance ─────────►│── route ──────────►│                     │               │
  │                     │                    │── Feign GET resume ─►│               │
  │                     │                    │◄── ResumeResponse ──│               │
  │                     │                    │── Feign GET jd ─────►│               │
  │                     │                    │◄── JDResponse ──────│               │
  │                     │                    │                                      │
  │                     │                    │── ChatClient ───────────────────────►│
  │                     │                    │   "Compare resume vs JD..."          │
  │                     │                    │◄── JSON response ───────────────────│
  │                     │                    │                                      │
  │                     │                    │── parse report                       │
  │                     │                    │── save enhancement_report            │
  │◄── 201 Report ──────│◄───────────────────│                                     │
```

---

## 8. AI Integration Details

### 8.1 Spring AI Configuration

Each AI-enabled service includes an `AIConfiguration` class that creates a `ChatClient` bean from the auto-configured `ChatClient.Builder`. The builder reads connection settings from `spring.ai.openai.*` properties.

```java
@Configuration
public class AIConfiguration {
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
```

### 8.2 Groq API Integration

The Groq API is OpenAI-API-compatible, so Spring AI's `spring-ai-openai-spring-boot-starter` works out of the box by overriding the base URL:

```yaml
spring:
  ai:
    openai:
      api-key: ${GROQ_API_KEY}
      base-url: https://api.groq.com/openai
      chat:
        options:
          model: llama-3.3-70b-versatile
```

### 8.3 Temperature Settings

| Service | Temperature | Rationale |
|---------|------------|-----------|
| Resume Service | 0.3 | Low creativity — deterministic JSON extraction from resume text |
| Interview Service | 0.7 | Higher creativity — varied, diverse interview questions |
| Profile Enhancer Service | 0.7 | Higher creativity — nuanced suggestions and rewritten content |

### 8.4 Prompt Engineering

All prompts instruct the AI to return **valid JSON only** with no additional text. This enables reliable parsing with Jackson's `ObjectMapper`. Each service includes fallback handling: if JSON parsing fails, the raw AI response is stored to prevent data loss.

---

## 9. Infrastructure & Deployment

### 9.1 Docker Compose Services

| Service | Image / Build | Port | Memory Limit |
|---------|--------------|------|-------------|
| `postgres` | `postgres:16` | 5432 | — |
| `config-server` | Built from `resume-ai-config-server` | 8888 | 512 MB |
| `discovery-server` | Built from `resume-ai-discovery-server` | 8761 | 512 MB |
| `api-gateway` | Built from `resume-ai-api-gateway` | 8080 | 512 MB |
| `resume-service` | Built from `resume-ai-resume-service` | 8081 | 512 MB |
| `interview-service` | Built from `resume-ai-interview-service` | 8082 | 512 MB |
| `profile-enhancer-service` | Built from `resume-ai-profile-enhancer-service` | 8083 | 512 MB |

### 9.2 Dockerfile Strategy

A **single shared Dockerfile** (`docker/Dockerfile`) is used by all services via build arguments:

- **Build stage** (`eclipse-temurin:17-jdk`): Copies Maven wrapper, parent POM, all module POMs (for dependency caching), then only the target module's source code. Builds with `./mvnw -pl ${MODULE_NAME} -am clean package -DskipTests`.
- **Runtime stage** (`eclipse-temurin:17-jre`): Copies the built JAR as `app.jar` and runs it.

This approach minimizes image size while allowing Docker layer caching for dependencies.

### 9.3 Environment Variables

| Variable | Purpose | Required |
|----------|---------|----------|
| `GROQ_API_KEY` | Groq API authentication key | Yes (for AI features) |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | No (defaults to `jdbc:postgresql://localhost:5432/resumeai`) |
| `SPRING_DATASOURCE_USERNAME` | Database username | No (defaults to `resumeai`) |
| `SPRING_DATASOURCE_PASSWORD` | Database password | No (defaults to `resumeai`) |
| `CONFIG_SERVER_URL` | Config Server URL | No (defaults to `http://localhost:8888/`) |
| `EUREKA_SERVER_URL` | Eureka Server URL | No (defaults to `http://localhost:8761/eureka/`) |

### 9.4 Health Checks

| Service | Healthcheck Command | Interval | Retries |
|---------|-------------------|----------|---------|
| PostgreSQL | `pg_isready -U resumeai` | 5s | 10 |
| Config Server | `curl -f http://localhost:8888/actuator/health` | 5s | 10 |
| Discovery Server | `curl -f http://localhost:8761/actuator/health` | 5s | 10 |

All Spring Boot services expose `/actuator/health`, `/actuator/info`, and `/actuator/metrics` endpoints via Spring Boot Actuator.

---

## 10. Build & Run Instructions

### 10.1 Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- Groq API Key (free at https://console.groq.com)

### 10.2 Build

```bash
cd resume-ai-platform
./mvnw clean install -DskipTests
```

### 10.3 Run with Docker Compose

```bash
export GROQ_API_KEY=your-api-key-here
docker compose up --build
```

### 10.4 Run Locally (without Docker)

Start a PostgreSQL instance, then launch services in order:

```bash
# 1. Config Server
cd resume-ai-config-server && ../mvnw spring-boot:run

# 2. Discovery Server
cd resume-ai-discovery-server && ../mvnw spring-boot:run

# 3. API Gateway
cd resume-ai-api-gateway && ../mvnw spring-boot:run

# 4. Resume Service
cd resume-ai-resume-service && ../mvnw spring-boot:run

# 5. Interview Service
cd resume-ai-interview-service && ../mvnw spring-boot:run

# 6. Profile Enhancer Service
cd resume-ai-profile-enhancer-service && ../mvnw spring-boot:run
```

### 10.5 Verification

```bash
# Check Eureka dashboard
open http://localhost:8761

# Upload a resume
curl -F "file=@resume.pdf" http://localhost:8080/api/resumes/upload

# Generate interview questions
curl -X POST http://localhost:8080/api/interviews/generate \
  -H "Content-Type: application/json" \
  -d '{"resumeId":"<uuid>","difficulty":"MIXED","numberOfQuestions":5}'

# Enhance profile against a JD
curl -X POST http://localhost:8080/api/enhance \
  -H "Content-Type: application/json" \
  -d '{"resumeId":"<uuid>","jdId":"<uuid>"}'

# Quick enhancement (no JD)
curl -X POST http://localhost:8080/api/enhance/quick \
  -H "Content-Type: application/json" \
  -d '{"resumeId":"<uuid>"}'
```

---

## 11. Project Structure

```
resume-ai-platform/
├── pom.xml                              # Parent POM (Spring Boot 3.2.5, Spring Cloud, Spring AI BOMs)
├── docker-compose.yml                   # Full orchestration (7 services)
├── docker/Dockerfile                    # Shared multi-stage Dockerfile
├── resume-ai-config-server/             # Centralized configuration (port 8888)
│   ├── pom.xml
│   └── src/main/
│       ├── java/.../ConfigServerApplication.java
│       └── resources/
│           ├── application.yml
│           └── config/                  # Configs served to all services
│               ├── application.yml
│               ├── resume-service.yml
│               ├── interview-service.yml
│               └── profile-enhancer-service.yml
├── resume-ai-discovery-server/          # Eureka registry (port 8761)
├── resume-ai-api-gateway/               # Edge routing (port 8080)
│   └── src/main/java/.../
│       ├── ApiGatewayApplication.java
│       └── CorsConfiguration.java       # Global CORS filter
├── resume-ai-resume-service/            # Resume + JD management (port 8081)
│   └── src/main/
│       ├── java/.../resume/
│       │   ├── controller/              # ResumeController, JobDescriptionController
│       │   ├── service/                 # ResumeParsingService, JobDescriptionService
│       │   ├── model/                   # Resume, JobDescription (JPA entities)
│       │   ├── repository/              # JpaRepository interfaces
│       │   ├── dto/                     # ResumeResponse, JobDescriptionResponse
│       │   ├── config/                  # AIConfiguration (ChatClient bean)
│       │   └── exception/               # GlobalExceptionHandler, ResourceNotFoundException
│       └── resources/db/migration/      # V1__init_resume_tables.sql
├── resume-ai-interview-service/         # Question generation (port 8082)
│   └── src/main/
│       ├── java/.../interview/
│       │   ├── controller/              # InterviewController
│       │   ├── service/                 # InterviewQuestionGeneratorService
│       │   ├── model/                   # InterviewSession, InterviewQuestion
│       │   ├── repository/              # JpaRepository interfaces
│       │   ├── client/                  # ResumeServiceClient (@FeignClient)
│       │   ├── dto/                     # GenerateRequest, InterviewSessionResponse
│       │   ├── config/                  # AIConfiguration
│       │   └── exception/               # GlobalExceptionHandler
│       └── resources/db/migration/      # V1__init_interview_tables.sql
└── resume-ai-profile-enhancer-service/  # Profile enhancement (port 8083)
    └── src/main/
        ├── java/.../enhancer/
        │   ├── controller/              # ProfileEnhancerController
        │   ├── service/                 # ProfileEnhancerService
        │   ├── model/                   # EnhancementReport
        │   ├── repository/              # JpaRepository interface
        │   ├── client/                  # ResumeServiceClient (@FeignClient)
        │   ├── dto/                     # EnhanceRequest, EnhancementReportResponse
        │   ├── config/                  # AIConfiguration
        │   └── exception/               # GlobalExceptionHandler
        └── resources/db/migration/      # V1__init_enhancement_tables.sql
```

---

## 12. Testing Strategy

Each service includes unit tests for the service layer and controller layer:

- **Service tests** — Use `@ExtendWith(MockitoExtension.class)` with `@Mock` for `ChatClient`, Feign clients, and repositories. Verify business logic and AI response parsing in isolation.
- **Controller tests** — Use `@WebMvcTest` with `@MockBean` for service dependencies. Verify HTTP status codes, request mapping, and response serialization.

Test files:
- `ResumeParsingServiceTest`, `ResumeControllerTest`
- `InterviewQuestionGeneratorServiceTest`, `InterviewControllerTest`
- `ProfileEnhancerServiceTest`, `ProfileEnhancerControllerTest`
