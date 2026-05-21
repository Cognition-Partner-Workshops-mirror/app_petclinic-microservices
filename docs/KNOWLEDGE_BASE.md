# Knowledge Base — Spring PetClinic Microservices Config

<!-- This document provides a comprehensive architecture overview of the Spring PetClinic
     Microservices system and its externalized configuration repository. -->

## 1. Architecture Overview

### 1.1 System Purpose

Spring PetClinic Microservices is a reference implementation of a distributed veterinary clinic
management system built on **Spring Boot 4.0.1** and **Spring Cloud 2025.1.0**. This repository
(`spring-petclinic-microservices-config`) serves as the **externalized configuration store** consumed
by a Spring Cloud Config Server, providing environment-specific settings for every microservice.

### 1.2 Service Inventory

<!-- Each service below is configured via a dedicated YAML file in this repository plus the
     shared application.yml that applies to all services. -->

| Service | Config File | Default Port | Technology Stack | Description |
|---|---|---|---|---|
| **Config Server** | _N/A (self-hosted)_ | 8888 | Spring Cloud Config Server | Serves this repo's YAML files to all services |
| **Discovery Server** | `discovery-server.yml` | 8761 | Netflix Eureka | Service registry; standalone mode (`registerWithEureka: false`) |
| **API Gateway** | `api-gateway.yml` | 8080 | Spring Cloud Gateway (WebFlux) | Edge router with circuit breakers, load balancing, response compression |
| **Customers Service** | `customers-service.yml` | 8081 (docker) / random (default) | Spring Boot WebMVC, JPA, HSQLDB/MySQL | Manages owners and pets |
| **Visits Service** | `visits-service.yml` | 8082 (docker) / random (default) | Spring Boot WebMVC, JPA, HSQLDB/MySQL | Manages pet visits |
| **Vets Service** | `vets-service.yml` | 8083 (docker) / random (default) | Spring Boot WebMVC, JPA, HSQLDB/MySQL | Manages veterinarians and specialties; Caffeine cache |
| **GenAI Service** | `genai-service.yml` | 8084 (docker) / random (default) | Spring AI, WebFlux, OpenAI/Azure OpenAI | LLM-powered chat assistant with tool-calling and RAG |
| **Admin Server** | `admin-server.yml` | 9090 | Spring Boot Admin | Monitoring dashboard for all registered services |
| **Tracing Server** | `tracing-server.yml` | 9411 | Zipkin | Distributed tracing collector |

### 1.3 Communication Patterns

```
┌─────────────┐     HTTP/REST      ┌──────────────────┐
│   Browser    │ ──────────────────>│   API Gateway    │
└─────────────┘                    │   (port 8080)    │
                                   └────────┬─────────┘
                          ┌─────────────────┼──────────────────┐
                          │ lb://           │ lb://            │ lb://
                   ┌──────▼──────┐  ┌──────▼───────┐  ┌──────▼──────┐
                   │  Customers  │  │    Visits     │  │    Vets     │
                   │  Service    │  │   Service     │  │   Service   │
                   └──────┬──────┘  └──────┬───────┘  └──────┬──────┘
                          │                │                  │
                   ┌──────▼──────┐  ┌──────▼───────┐  ┌──────▼──────┐
                   │  HSQLDB /   │  │  HSQLDB /    │  │  HSQLDB /   │
                   │  MySQL      │  │  MySQL       │  │  MySQL      │
                   └─────────────┘  └──────────────┘  └─────────────┘
```

<!-- Services discover each other via Eureka and communicate over HTTP/REST.
     The API Gateway uses Spring Cloud LoadBalancer (lb://) for client-side load balancing. -->

- **Service Discovery**: All services register with Eureka (`discovery-server:8761`) and resolve
  each other using logical service names (e.g., `lb://customers-service`).
- **Gateway Routing**: The API Gateway routes external requests via path-based predicates
  (`/api/customer/**` → `customers-service`, `/api/vet/**` → `vets-service`, etc.).
- **Inter-service Calls**: The API Gateway's `ApiGatewayController` aggregates data from Customers
  and Visits services using reactive `WebClient`. The GenAI service calls Customers and Vets services
  using synchronous `RestClient` and `WebClient`.
- **Circuit Breaking**: Resilience4j reactive circuit breakers protect cross-service calls in the
  gateway. A `FallbackController` returns HTTP 503 for the GenAI service fallback.

### 1.4 Configuration Architecture

```
spring-petclinic-microservices-config/        ← This repository
├── application.yml          ← Shared config applied to ALL services
├── customers-service.yml    ← Customers-specific overrides
├── visits-service.yml       ← Visits-specific overrides
├── vets-service.yml         ← Vets-specific overrides (cache TTL)
├── genai-service.yml        ← GenAI-specific overrides
├── api-gateway.yml          ← Gateway-specific (compression, i18n)
├── admin-server.yml         ← Admin Server port + Eureka URL
├── discovery-server.yml     ← Eureka standalone configuration
└── tracing-server.yml       ← Zipkin port + Eureka registration
```

<!-- The Config Server can operate in two modes:
     1. Git-backed (default) — clones this repo from GitHub
     2. Native/file-system — reads from a local checkout via GIT_REPO env var -->

**Spring Profiles**:

| Profile | Purpose |
|---|---|
| `default` | Local development; services use random ports; HSQLDB in-memory DB |
| `docker` | Docker Compose; fixed ports; Eureka at `discovery-server:8761`; Zipkin at `tracing-server:9411` |
| `mysql` | Switches from HSQLDB to MySQL (`jdbc:mysql://localhost:3306/petclinic`) |
| `chaos-monkey` | Enables Chaos Monkey for fault injection |
| `production` | Enables caching in the Vets Service |

---

## 2. Data Models

### 2.1 Customers Service

<!-- The Customers Service owns three JPA entities stored in a single database. -->

#### Owner Entity (`owners` table)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `Integer` | PK, auto-generated | Unique owner identifier |
| `first_name` | `String` | `@NotBlank` | Owner's first name |
| `last_name` | `String` | `@NotBlank` | Owner's last name |
| `address` | `String` | `@NotBlank` | Street address |
| `city` | `String` | `@NotBlank` | City |
| `telephone` | `String` | `@NotBlank`, `@Digits(fraction=0, integer=12)` | Phone number |

**Relationships**: `Owner` → `Pet` (OneToMany, cascade ALL, eager fetch)

#### Pet Entity (`pets` table)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `Integer` | PK, auto-generated | Unique pet identifier |
| `name` | `String` | — | Pet name |
| `birth_date` | `Date` | `@Temporal(DATE)` | Date of birth |
| `type_id` | FK → `types` | `@ManyToOne` | Pet type (cat, dog, etc.) |
| `owner_id` | FK → `owners` | `@ManyToOne`, `@JsonIgnore` | Owning customer |

#### PetType Entity (`types` table)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `Integer` | PK, auto-generated | Type identifier |
| `name` | `String` | — | Type name (cat, dog, lizard, snake, bird, hamster) |

### 2.2 Vets Service

#### Vet Entity (`vets` table)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `Integer` | PK, auto-generated | Unique vet identifier |
| `first_name` | `String` | `@NotBlank` | Vet's first name |
| `last_name` | `String` | `@NotBlank` | Vet's last name |

**Relationships**: `Vet` → `Specialty` (ManyToMany via `vet_specialties` join table, eager fetch)

#### Specialty Entity (`specialties` table)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `Integer` | PK, auto-generated | Specialty identifier |
| `name` | `String` | — | Specialty name (e.g., dentistry, radiology, surgery) |

### 2.3 Visits Service

#### Visit Entity (`visits` table)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `Integer` | PK, auto-generated | Unique visit identifier |
| `visit_date` | `Date` | `@Temporal(TIMESTAMP)`, JSON format `yyyy-MM-dd` | Visit date |
| `description` | `String` | `@Size(max=8192)` | Visit description |
| `pet_id` | `int` | — | References pet (no FK constraint — cross-service) |

---

## 3. API Surface Map

### 3.1 Customers Service Endpoints

| Method | Path | Handler | Request Body | Response | Description |
|---|---|---|---|---|---|
| `POST` | `/owners` | `OwnerResource.createOwner` | `OwnerRequest` (firstName, lastName, address, city, telephone) | `Owner` (201) | Create owner |
| `GET` | `/owners` | `OwnerResource.findAll` | — | `List<Owner>` | List all owners |
| `GET` | `/owners/{ownerId}` | `OwnerResource.findOwner` | — | `Optional<Owner>` | Get owner by ID |
| `PUT` | `/owners/{ownerId}` | `OwnerResource.updateOwner` | `OwnerRequest` | — (204) | Update owner |
| `GET` | `/petTypes` | `PetResource.getPetTypes` | — | `List<PetType>` | List all pet types |
| `POST` | `/owners/{ownerId}/pets` | `PetResource.processCreationForm` | `PetRequest` (id, name, birthDate, typeId) | `Pet` (201) | Add pet to owner |
| `PUT` | `/owners/*/pets/{petId}` | `PetResource.processUpdateForm` | `PetRequest` | — (204) | Update pet |
| `GET` | `/owners/*/pets/{petId}` | `PetResource.findPet` | — | `PetDetails` | Get pet details |

### 3.2 Visits Service Endpoints

| Method | Path | Handler | Request Body | Response | Description |
|---|---|---|---|---|---|
| `POST` | `/owners/*/pets/{petId}/visits` | `VisitResource.create` | `Visit` (date, description) | `Visit` (201) | Create visit for pet |
| `GET` | `/owners/*/pets/{petId}/visits` | `VisitResource.read` | — | `List<Visit>` | Get visits for a pet |
| `GET` | `/pets/visits?petId=1,2,3` | `VisitResource.read` | — | `Visits` (items list) | Batch-get visits by pet IDs |

### 3.3 Vets Service Endpoints

| Method | Path | Handler | Request Body | Response | Description |
|---|---|---|---|---|---|
| `GET` | `/vets` | `VetResource.showResourcesVetList` | — | `List<Vet>` | List all vets (cached) |

### 3.4 GenAI Service Endpoints

| Method | Path | Handler | Request Body | Response | Description |
|---|---|---|---|---|---|
| `POST` | `/chatclient` | `PetclinicChatClient.exchange` | `String` (user query) | `String` (LLM response) | Chat with LLM assistant |

**LLM Tool Functions** (invoked by the AI model, not directly via HTTP):

| Tool | Description |
|---|---|
| `listOwners` | Returns all owners from Customers Service |
| `addOwnerToPetclinic` | Creates a new owner |
| `listVets` | Searches vets via vector store similarity (RAG) |
| `addPetToOwner` | Adds a pet to an owner |

### 3.5 API Gateway Routes

<!-- The gateway routes are defined in the api-gateway application.yml, not in this config repo. -->

| Route ID | External Path | Target Service | Filters |
|---|---|---|---|
| `vets-service` | `/api/vet/**` | `lb://vets-service` | StripPrefix=2 |
| `visits-service` | `/api/visit/**` | `lb://visits-service` | StripPrefix=2 |
| `customers-service` | `/api/customer/**` | `lb://customers-service` | StripPrefix=2 |
| `genai-service` | `/api/genai/**` | `lb://genai-service` | StripPrefix=2, CircuitBreaker |

**Gateway Aggregation Endpoint**:

| Method | Path | Handler | Description |
|---|---|---|---|
| `GET` | `/api/gateway/owners/{ownerId}` | `ApiGatewayController.getOwnerDetails` | Aggregates owner + visits data with circuit breaker |

### 3.6 Actuator Endpoints (All Services)

<!-- All actuator endpoints are exposed via management.endpoints.web.exposure.include: "*" in application.yml -->

All services expose the full Spring Boot Actuator surface:
- `/actuator/health` — health checks
- `/actuator/info` — build info
- `/actuator/metrics` — Micrometer metrics
- `/actuator/prometheus` — Prometheus scrape endpoint

---

## 4. Business Logic Inventory

### 4.1 Owner Management (Customers Service)

<!-- Business rules are enforced via Jakarta Bean Validation annotations on entities
     and request DTOs. -->

- **Create/Update Owner**: Validated fields (firstName, lastName, address, city, telephone all
  required). Owner-to-request mapping via `OwnerEntityMapper` (MapStruct-style interface).
- **Pet Association**: Pets are always created under an owner context. The `Owner.addPet()` method
  maintains the bidirectional relationship.
- **Pet Type Lookup**: Pet types are reference data queried via JPQL (`findPetTypes`, `findPetTypeById`).

### 4.2 Visit Tracking (Visits Service)

- **Create Visit**: Associates visit with a pet via `petId` path variable. Default visit date is
  set to current date (`new Date()`).
- **Batch Retrieval**: `findByPetIdIn` enables efficient batch loading of visits for multiple pets
  (used by the API Gateway aggregation endpoint).

### 4.3 Veterinarian Directory (Vets Service)

- **Cached List**: Vet list is cached using Spring's `@Cacheable("vets")` annotation with Caffeine
  backend. Cache TTL is configured at 60 seconds with a heap size of 100 entries (`vets-service.yml`).
- **Specialty Sorting**: Specialties are sorted alphabetically via `PropertyComparator`.

### 4.4 AI Chat Assistant (GenAI Service)

- **Chat Flow**: User messages are sent to the LLM (GPT-4o-mini via OpenAI or GPT-4o via Azure
  OpenAI) with a system prompt defining the assistant's persona.
- **Tool Calling**: The LLM can invoke `PetclinicTools` methods to query/modify clinic data in real
  time via Customers and Vets services.
- **RAG (Retrieval-Augmented Generation)**: Vet data is loaded into a `SimpleVectorStore` at startup
  (from a pre-embedded `vectorstore.json` or fetched live). Similarity search provides context for
  vet-related queries.
- **Chat Memory**: Up to 10 previous messages are retained via `MessageChatMemoryAdvisor`.

### 4.5 API Gateway Aggregation

- **Owner Details Enrichment**: `ApiGatewayController.getOwnerDetails` fetches an owner from the
  Customers Service, then fetches visits for all the owner's pets from the Visits Service. Visits
  are merged into each pet's visit list. A circuit breaker returns empty visits on failure.

---

## 5. Integration Points

### 5.1 Service Discovery — Eureka

- **Server**: `discovery-server.yml` configures standalone Eureka at port 8761.
- **Clients**: All services register with Eureka. In `docker` profile, the Eureka URL is
  `http://discovery-server:8761/eureka/`. In default profile, services use random ports with UUID
  instance IDs for multi-instance support.

### 5.2 Centralized Configuration — Spring Cloud Config

- **Server**: Fetches config from this Git repository
  (`https://github.com/spring-petclinic/spring-petclinic-microservices-config`) or a local file
  system via the `native` profile.
- **Clients**: All services import config via `optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888/}`.
- **Override Policy**: `allow-override: true` and `override-none: true` let local properties take
  precedence over remote ones.

### 5.3 Distributed Tracing — Zipkin

- **Sampling**: 100% sampling rate (`spring.sleuth.sampler.probability: 1.0` and
  `management.tracing.sampling.probability: 1`).
- **Transport**: In `docker` profile, traces are exported to `http://tracing-server:9411/api/v2/spans`.
- **Instrumentation**: Micrometer Tracing with Brave bridge + Zipkin reporter. OpenTelemetry Zipkin
  exporter is also on the classpath.

### 5.4 Monitoring & Metrics

- **Prometheus**: All services export metrics at `/actuator/prometheus` via `micrometer-registry-prometheus`.
- **Grafana**: Docker Compose includes a Grafana instance (port 3030) pre-configured with Prometheus
  data source (port 9091).
- **Spring Boot Admin**: Admin Server at port 9090 provides a web UI for monitoring all registered
  services.
- **Custom Metrics**: `@Timed` annotations on controllers track request durations
  (`petclinic.owner`, `petclinic.pet`, `petclinic.visit`). `MetricConfig` beans configure
  `datasource-micrometer` for JDBC query observation.

### 5.5 Database Connectivity

- **Default**: In-memory HSQLDB with schema/data initialization from `db/hsqldb/schema.sql` and
  `db/hsqldb/data.sql`.
- **MySQL Profile**: `jdbc:mysql://localhost:3306/petclinic` with `root/petclinic` credentials.
  Initialization from `db/mysql/schema.sql` and `db/mysql/data.sql`. Mode: `ALWAYS`.
- **JPA Settings**: `open-in-view: false`, `ddl-auto: none` (schema managed by SQL scripts).

### 5.6 AI/LLM Provider

- **OpenAI**: API key via `OPENAI_API_KEY` env var; model `gpt-4o-mini`; temperature 0.7.
- **Azure OpenAI**: API key via `AZURE_OPENAI_KEY`; endpoint via `AZURE_OPENAI_ENDPOINT`;
  deployment `gpt-4o`.
- **Vector Store**: `SimpleVectorStore` (in-memory) for vet data RAG.

### 5.7 Chaos Engineering

- **Chaos Monkey for Spring Boot** (`chaos-monkey` profile): Configurable watchers for components,
  controllers, repositories, rest-controllers, and services. All watchers are disabled by default
  in configuration — enabled per experiment.

---

## 6. Build & Deployment Summary

### 6.1 Build System

- **Maven** multi-module project with Maven Wrapper (`./mvnw`).
- **Java 17** required.
- **Parent POM**: `spring-boot-starter-parent:4.0.1`.
- **BOM**: `spring-cloud-dependencies:2025.1.0`.

### 6.2 Key Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-data-jpa` | JPA/Hibernate for domain services |
| `spring-cloud-starter-config` | Config Server client |
| `spring-cloud-starter-netflix-eureka-client` | Eureka service discovery |
| `spring-cloud-starter-gateway-server-webflux` | API Gateway (reactive) |
| `spring-cloud-starter-circuitbreaker-reactor-resilience4j` | Circuit breakers |
| `spring-ai-starter-model-openai` | OpenAI integration (GenAI) |
| `micrometer-registry-prometheus` | Prometheus metrics export |
| `micrometer-tracing-bridge-brave` | Distributed tracing |
| `chaos-monkey-spring-boot` | Chaos engineering |
| `datasource-micrometer-spring-boot` | JDBC query observation |

### 6.3 Docker Deployment

- **Docker Compose** orchestrates all services with health checks and dependency ordering:
  `config-server` → `discovery-server` → domain services + gateway.
- **Images**: Published under `springcommunity/` prefix.
- **Memory Limits**: 512 MB per service; 256 MB for Grafana/Prometheus.
- **Build**: Multi-platform Docker images via `exec-maven-plugin` (supports `linux/amd64` and
  `linux/arm64`).

### 6.4 This Config Repository

- **License**: Apache 2.0.
- **Structure**: Flat directory with one YAML file per service + shared `application.yml`.
- **Branching**: `main` branch is the default label for Config Server.
- **No build pipeline**: Pure configuration — no CI/CD, tests, or build artifacts.
