# Spring PetClinic Microservices — Knowledge Base

## 1. Architecture Overview

### 1.1 System Summary

Spring PetClinic Microservices is a distributed, cloud-native application for managing a veterinary clinic. It demonstrates Spring Cloud patterns including service discovery, centralized configuration, API gateway routing, circuit breakers, distributed tracing, and AI-powered chatbot integration.

**Technology Stack:**

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.0.1, Spring Cloud 2025.1.0 |
| AI | Spring AI 2.0.0-M1 (OpenAI / Azure OpenAI) |
| Service Discovery | Netflix Eureka |
| Configuration | Spring Cloud Config Server (Git / Native) |
| API Gateway | Spring Cloud Gateway (WebFlux) |
| Resilience | Resilience4j (Circuit Breaker, Retry) |
| Database | HSQLDB (default in-memory), MySQL 8.4.5 (optional) |
| ORM | Spring Data JPA / Hibernate |
| Metrics | Micrometer, Prometheus, Grafana |
| Tracing | OpenTelemetry, Zipkin, Brave |
| Monitoring | Spring Boot Admin, Jolokia |
| Chaos Engineering | Chaos Monkey for Spring Boot 3.1.0 |
| Frontend | AngularJS 1.8.3 (served via API Gateway webjars) |
| Build | Maven (wrapper), multi-module aggregator POM |
| Containerization | Docker / Podman, multi-stage Dockerfile, docker-compose |

### 1.2 Service Inventory

The system comprises **8 deployable modules**:

| # | Service | Type | Default Port | Description |
|---|---|---|---|---|
| 1 | `spring-petclinic-config-server` | Infrastructure | 8888 | Centralized configuration via Git or local filesystem |
| 2 | `spring-petclinic-discovery-server` | Infrastructure | 8761 | Eureka service registry for dynamic discovery |
| 3 | `spring-petclinic-api-gateway` | Edge | 8080 | Reactive gateway, routes requests, serves AngularJS UI |
| 4 | `spring-petclinic-customers-service` | Domain | Random (8081 in Docker) | Manages owners and pets |
| 5 | `spring-petclinic-vets-service` | Domain | Random (8083 in Docker) | Manages veterinarians and specialties |
| 6 | `spring-petclinic-visits-service` | Domain | Random (8082 in Docker) | Manages pet visit records |
| 7 | `spring-petclinic-genai-service` | Domain | Random (8084 in Docker) | AI chatbot using Spring AI + OpenAI |
| 8 | `spring-petclinic-admin-server` | Monitoring | 9090 | Spring Boot Admin dashboard |

**Supporting infrastructure (Docker only):**

| Component | Image | Port |
|---|---|---|
| Zipkin (Tracing) | `openzipkin/zipkin` | 9411 |
| Grafana | Custom build (`docker/grafana`) | 3030 |
| Prometheus | Custom build (`docker/prometheus`) | 9091 |

### 1.3 Communication Patterns

```
┌──────────┐     ┌──────────────┐     ┌──────────────────┐
│  Browser  │────▶│  API Gateway │────▶│ customers-service │
│ (Angular) │     │   (8080)     │────▶│ vets-service      │
└──────────┘     │              │────▶│ visits-service     │
                  │              │────▶│ genai-service      │
                  └──────┬───────┘     └────────┬──────────┘
                         │                      │
                  ┌──────▼───────┐      ┌───────▼─────────┐
                  │   Eureka     │◀─────│  All services    │
                  │   (8761)     │      │  register here   │
                  └──────────────┘      └─────────────────┘
                         ▲
                  ┌──────┴───────┐
                  │ Config Server│
                  │   (8888)     │
                  └──────────────┘
```

- **Gateway → Domain services**: HTTP via Spring Cloud Gateway routes with load-balanced URIs (`lb://service-name`).
- **API Gateway Controller**: Uses reactive `WebClient` with `@LoadBalanced` to call customers-service and visits-service, merging responses.
- **GenAI → customers-service**: Synchronous `RestClient` calls via Eureka `DiscoveryClient` for owner/pet CRUD.
- **GenAI → vets-service**: Reactive `WebClient` (load-balanced) to fetch vet data for vector store.
- **All services → Config Server**: Pull configuration at startup via `spring.config.import`.
- **All services → Eureka**: Register on startup, heartbeat for availability.
- **Circuit Breaker**: Resilience4j wraps the visits-service call in the gateway controller and the genai-service route.

### 1.4 Startup Order

1. **Config Server** (port 8888) — must be healthy first
2. **Discovery Server** (port 8761) — depends on Config Server
3. **Domain services** (customers, vets, visits, genai) — depend on both Config and Discovery
4. **API Gateway** (port 8080) — depends on both Config and Discovery
5. **Admin Server** (port 9090) — depends on both Config and Discovery

---

## 2. Data Models

### 2.1 Customers Service (owners, pets, types)

#### Entity: `Owner` (`owners` table)

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `Integer` | PK, auto-generated | `IDENTITY` strategy |
| `firstName` | `String` | `@NotBlank` | `first_name` column |
| `lastName` | `String` | `@NotBlank` | `last_name` column |
| `address` | `String` | `@NotBlank` | |
| `city` | `String` | `@NotBlank` | |
| `telephone` | `String` | `@NotBlank`, `@Digits(fraction=0, integer=12)` | |
| `pets` | `Set<Pet>` | `@OneToMany(cascade=ALL, fetch=EAGER)` | Bidirectional, mapped by `owner` |

#### Entity: `Pet` (`pets` table)

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `Integer` | PK, auto-generated | |
| `name` | `String` | | |
| `birthDate` | `Date` | `@Temporal(DATE)` | `birth_date` column |
| `type` | `PetType` | `@ManyToOne`, FK `type_id` | |
| `owner` | `Owner` | `@ManyToOne`, FK `owner_id`, `@JsonIgnore` | |

#### Entity: `PetType` (`types` table)

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `Integer` | PK, auto-generated | |
| `name` | `String` | | Cat, Dog, Lizard, Snake, Bird, Hamster |

**Relationships:**
- `Owner` 1:N `Pet` (cascade ALL, eager fetch)
- `Pet` N:1 `PetType`
- `Pet` N:1 `Owner`

### 2.2 Vets Service (vets, specialties)

#### Entity: `Vet` (`vets` table)

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `Integer` | PK, auto-generated | |
| `firstName` | `String` | `@NotBlank` | `first_name` column |
| `lastName` | `String` | `@NotBlank` | `last_name` column |
| `specialties` | `Set<Specialty>` | `@ManyToMany(fetch=EAGER)` | Join table `vet_specialties` |

#### Entity: `Specialty` (`specialties` table)

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `Integer` | PK, auto-generated | |
| `name` | `String` | | e.g., radiology, surgery, dentistry |

**Relationships:**
- `Vet` M:N `Specialty` via `vet_specialties` join table

### 2.3 Visits Service (visits)

#### Entity: `Visit` (`visits` table)

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `Integer` | PK, auto-generated | |
| `date` | `Date` | `@Temporal(TIMESTAMP)`, `@JsonFormat("yyyy-MM-dd")` | `visit_date` column, defaults to `new Date()` |
| `description` | `String` | `@Size(max=8192)` | |
| `petId` | `int` | | `pet_id` column — no FK constraint, cross-service reference |

**Note:** The `petId` in the visits service references pets in the customers service. There is no database-level foreign key — this is a cross-service reference by convention.

### 2.4 Database Schema Summary

```
customers-service DB:          vets-service DB:           visits-service DB:
┌─────────┐  ┌──────┐         ┌──────┐  ┌────────────┐   ┌────────┐
│ owners  │──│ pets │         │ vets │──│specialties │   │ visits │
└─────────┘  └──┬───┘         └──────┘  └────────────┘   └────────┘
                 │                  M:N via vet_specialties
              ┌──▼───┐
              │types │
              └──────┘
```

---

## 3. API Surface Map

### 3.1 API Gateway Routes (port 8080)

The gateway proxies incoming requests to backend services:

| Route Pattern | Target Service | Strip Prefix | Notes |
|---|---|---|---|
| `/api/customer/**` | `lb://customers-service` | 2 | Customers + Pets |
| `/api/vet/**` | `lb://vets-service` | 2 | Veterinarians |
| `/api/visit/**` | `lb://visits-service` | 2 | Visits |
| `/api/genai/**` | `lb://genai-service` | 2 | AI Chatbot |

### 3.2 API Gateway Controller

| Method | Path | Description | Returns |
|---|---|---|---|
| `GET` | `/api/gateway/owners/{ownerId}` | Fetches owner with pets and visits (aggregated) | `Mono<OwnerDetails>` |

### 3.3 Fallback Controller (API Gateway)

| Method | Path | Description | Returns |
|---|---|---|---|
| `POST` | `/fallback` | Circuit breaker fallback endpoint | 503 Service Unavailable |

### 3.4 Customers Service Endpoints

#### OwnerResource (`/owners`)

| Method | Path | Request Body | Response | Status |
|---|---|---|---|---|
| `POST` | `/owners` | `OwnerRequest` (firstName, lastName, address, city, telephone) | `Owner` | 201 Created |
| `GET` | `/owners` | — | `List<Owner>` | 200 OK |
| `GET` | `/owners/{ownerId}` | — | `Optional<Owner>` | 200 OK |
| `PUT` | `/owners/{ownerId}` | `OwnerRequest` | — | 204 No Content |

#### PetResource

| Method | Path | Request Body | Response | Status |
|---|---|---|---|---|
| `GET` | `/petTypes` | — | `List<PetType>` | 200 OK |
| `POST` | `/owners/{ownerId}/pets` | `PetRequest` (id, birthDate, name, typeId) | `Pet` | 201 Created |
| `PUT` | `/owners/*/pets/{petId}` | `PetRequest` | — | 204 No Content |
| `GET` | `/owners/*/pets/{petId}` | — | `PetDetails` | 200 OK |

### 3.5 Vets Service Endpoints

#### VetResource (`/vets`)

| Method | Path | Request Body | Response | Status |
|---|---|---|---|---|
| `GET` | `/vets` | — | `List<Vet>` | 200 OK (cached) |

### 3.6 Visits Service Endpoints

#### VisitResource

| Method | Path | Request Body | Response | Status |
|---|---|---|---|---|
| `POST` | `/owners/*/pets/{petId}/visits` | `Visit` (date, description) | `Visit` | 201 Created |
| `GET` | `/owners/*/pets/{petId}/visits` | — | `List<Visit>` | 200 OK |
| `GET` | `/pets/visits?petId=1,2,3` | — | `Visits` (items list) | 200 OK |

### 3.7 GenAI Service Endpoints

#### PetclinicChatClient (`/`)

| Method | Path | Request Body | Response | Status |
|---|---|---|---|---|
| `POST` | `/chatclient` | `String` (natural language query) | `String` (AI response) | 200 OK |

**AI Tool Functions (invoked by LLM):**

| Function | Description | Delegates To |
|---|---|---|
| `listOwners()` | List all owners with pets | `GET /owners` on customers-service |
| `addOwnerToPetclinic(OwnerRequest)` | Create a new owner | `POST /owners` on customers-service |
| `listVets(Vet)` | Search vets via vector similarity | Vector store similarity search |
| `addPetToOwner(ownerId, PetRequest)` | Add a pet to an owner | `POST /owners/{id}/pets` on customers-service |

---

## 4. Business Logic Inventory

### 4.1 Owner Management (customers-service)
- **Create Owner**: Validates `@NotBlank` fields and `@Digits` telephone, maps `OwnerRequest` to `Owner` via `OwnerEntityMapper`, persists via JPA.
- **Update Owner**: Finds by ID (throws `ResourceNotFoundException` if missing), maps updated fields, saves.
- **List Owners**: Returns all owners with eagerly-fetched pets (sorted by name).

### 4.2 Pet Management (customers-service)
- **Add Pet**: Validates owner exists, creates `Pet`, looks up `PetType` by ID, associates with owner, persists.
- **Update Pet**: Finds pet by ID, updates name/birthDate/type.
- **Pet Types**: Returns all `PetType` entries ordered by name.

### 4.3 Vet Management (vets-service)
- **List Vets**: Returns all vets with eagerly-fetched specialties. Result is **cached** (`@Cacheable("vets")`) with configurable TTL (default 60s) and heap size (100 entries). Caching is only active under the `production` profile.

### 4.4 Visit Management (visits-service)
- **Create Visit**: Sets `petId` from path variable, persists visit with auto-generated date.
- **Read Visits by Pet**: Finds visits for a single pet ID.
- **Batch Read Visits**: Accepts comma-separated pet IDs, returns all matching visits — used by the API gateway to aggregate visits for an owner's pets.

### 4.5 AI Chatbot (genai-service)
- **Chat Interaction**: Accepts natural language queries, processes via Spring AI `ChatClient` with system prompt defining the PetClinic assistant persona.
- **Tool Calling**: LLM can invoke registered `@Tool` functions to list owners, add owners, list vets (via vector similarity), and add pets.
- **Vector Store**: On startup, loads vet data into a `SimpleVectorStore` (from pre-embedded JSON or fetched live from vets-service). Used for RAG-based vet queries.
- **Chat Memory**: Maintains up to 10 previous messages for conversational context.

### 4.6 API Gateway Aggregation
- **Owner Details**: Fetches owner from customers-service, then fetches visits for all owner's pets from visits-service. Merges visit data into pet details. Wrapped in a Resilience4j circuit breaker — returns empty visits on failure.

---

## 5. Integration Points

### 5.1 Service Discovery (Eureka)
- All domain services and the API gateway register with Eureka.
- Services use `lb://service-name` URIs for load-balanced inter-service calls.
- Config: `eureka.instance.prefer-ip-address: true`, random instance IDs for scaling.

### 5.2 Centralized Configuration (Config Server)
- Backed by a Git repository (`spring-petclinic-microservices-config`) or local filesystem (`native` profile).
- All services import config via `spring.config.import: optional:configserver:http://localhost:8888/`.
- Shared config in `application.yml` sets common properties (JPA, actuator, metrics, logging).

### 5.3 Distributed Tracing
- **Brave** bridge + **Zipkin** exporter via OpenTelemetry.
- Sampling probability: 1.0 (100% of traces captured).
- Zipkin endpoint: `http://tracing-server:9411/api/v2/spans` (Docker profile).

### 5.4 Metrics & Monitoring
- **Micrometer** with Prometheus registry — custom `petclinic` application tag.
- **Prometheus** scrapes `/actuator/prometheus` from gateway, customers, visits, and vets services.
- **Grafana** provides pre-configured dashboards.
- **Spring Boot Admin** discovers services via Eureka for health/actuator monitoring.
- **Jolokia** provides JMX-over-HTTP for all services.
- **Custom `@Timed` annotations**: `petclinic.owner`, `petclinic.pet`, `petclinic.visit`.

### 5.5 Chaos Engineering
- **Chaos Monkey for Spring Boot** is included in all domain services.
- Activated via the `chaos-monkey` profile.
- Configurable watchers for controllers, services, repositories, and REST controllers.

### 5.6 AI Integration (GenAI Service)
- **OpenAI** (default): Uses `OPENAI_API_KEY` env var, model `gpt-4o-mini`, temperature 0.7.
- **Azure OpenAI** (alternative): Uses `AZURE_OPENAI_KEY` and `AZURE_OPENAI_ENDPOINT`, model `gpt-4o`.
- Vector store uses OpenAI embedding model for vet similarity search.

### 5.7 Database Connections
- **Default**: In-memory HSQLDB — schema and data loaded at startup from `db/hsqldb/schema.sql` and `db/hsqldb/data.sql`.
- **MySQL profile**: Connects to `jdbc:mysql://localhost:3306/petclinic`, schema/data from `db/mysql/` scripts.
- Each domain service (customers, vets, visits) has its own isolated database.

---

## 6. Build & Deployment Summary

### 6.1 Build System
- **Maven** multi-module aggregator POM with wrapper (`./mvnw`).
- Root POM: `spring-boot-starter-parent:4.0.1`.
- Profiles:
  - `springboot` (auto-activated): Build info, git-commit-id plugin, Java version enforcement.
  - `buildDocker`: Builds OCI images via `exec-maven-plugin` calling `docker build`.

### 6.2 Docker Build
- **Multi-stage Dockerfile** (`docker/Dockerfile`):
  - Stage 1: `eclipse-temurin:17` — extracts Spring Boot layers from JAR.
  - Stage 2: `eclipse-temurin:17` — copies layers for optimal caching.
  - Entry point: `java org.springframework.boot.loader.launch.JarLauncher`.
- Supports `docker` and `podman` via `container.executable` property.
- Platform configurable: `linux/amd64` (default) or `linux/arm64`.

### 6.3 Docker Compose Orchestration
- All 8 services + Zipkin + Grafana + Prometheus.
- Health checks on Config Server and Discovery Server with `depends_on: service_healthy`.
- Memory limits: 512M per service, 256M for Grafana/Prometheus.
- `SPRING_PROFILES_ACTIVE=docker` set in Dockerfile.

### 6.4 CI Pipeline
- GitHub Actions workflow: `maven-build.yml`.
- Build command: `./mvnw clean install -DskipTests` (fast) or `./mvnw clean install` (with tests).

### 6.5 Key Build Commands

| Command | Purpose |
|---|---|
| `./mvnw clean install -DskipTests` | Build all modules (skip tests) |
| `./mvnw test` | Run all tests |
| `./mvnw clean install -P buildDocker` | Build Docker images |
| `docker compose up` | Start all services |
| `./scripts/run_all.sh` | Hybrid: Docker for infra, Java for apps |
