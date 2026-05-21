# Spring PetClinic Microservices — Remediation Roadmap

Gaps from the [Gap Analysis](GAP_ANALYSIS.md) are prioritized into three phases:
- **Phase 1 — Quick Wins**: High severity, small effort — immediate value.
- **Phase 2 — Important**: High severity, medium effort — core improvements.
- **Phase 3 — Polish**: Medium/low severity — long-term excellence.

Each item includes an actionable **Devin prompt** that can be executed directly.

---

## Phase 1: Quick Wins (1–2 weeks)

### 1.1 Secure Actuator Endpoints (Gap 4.2)
**Severity:** Critical | **Effort:** Small

Restrict actuator endpoint exposure to only health, info, and prometheus. Protect sensitive endpoints.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, update the shared configuration in the external config repo (`spring-petclinic-microservices-config/application.yml`) to change `management.endpoints.web.exposure.include` from `"*"` to `"health,info,prometheus,metrics"`. Remove the `management.security.enabled: false` line. Verify the change doesn't break the existing Prometheus scraping or Grafana dashboards by checking that `/actuator/prometheus` still works on all services.

---

### 1.2 Add Centralized Exception Handlers (Gap 2.1)
**Severity:** High | **Effort:** Small

Add `@ControllerAdvice` to each domain service for consistent error responses.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, create a `@ControllerAdvice` class named `GlobalExceptionHandler` in each domain service (customers-service, vets-service, visits-service) under a new `web` package (or existing `web` package). Handle `ResourceNotFoundException` (404), `MethodArgumentNotValidException` (400), `ConstraintViolationException` (400), and generic `Exception` (500). Return a consistent JSON error response with fields: `timestamp`, `status`, `error`, `message`, `path`. Add unit tests for each handler. Do not modify existing tests.

---

### 1.3 Fix `findOwner` Optional Handling (Gap 2.2)
**Severity:** High | **Effort:** Small

Unwrap the Optional and throw 404 when owner is not found.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, in `spring-petclinic-customers-service/src/main/java/.../web/OwnerResource.java`, change the `findOwner` method to unwrap the `Optional<Owner>` and throw `ResourceNotFoundException` if the owner is not found, instead of returning `Optional<Owner>` directly. Update the return type to `Owner`. Add a unit test in a new `OwnerResourceTest.java` verifying both the 200 (found) and 404 (not found) scenarios.

---

### 1.4 Add OpenAPI Documentation (Gap 5.3)
**Severity:** High | **Effort:** Small

Add Springdoc OpenAPI for auto-generated API docs.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, add `springdoc-openapi-starter-webmvc-ui` dependency to the customers-service, vets-service, and visits-service `pom.xml` files. Add `springdoc-openapi-starter-webflux-ui` to the api-gateway `pom.xml`. Configure the OpenAPI info (title: "Spring PetClinic - {service-name}", version: "4.0.1") in each service's `application.yml`. Verify that `/swagger-ui.html` and `/v3/api-docs` endpoints are accessible. Use only the open-source springdoc library — do not use any proprietary alternatives.

---

### 1.5 Add Timeouts to RestClient Calls (Gap 7.2)
**Severity:** High | **Effort:** Small

Configure connection and read timeouts on the GenAI service's RestClient.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, in `spring-petclinic-genai-service/src/main/java/.../genai/AIDataProvider.java`, configure the `RestClient` with a connection timeout of 5 seconds and a read timeout of 10 seconds using `HttpComponentsClientHttpRequestFactory` or `JdkClientHttpRequestFactory`. Also configure the `WebClient` in `AIBeanConfiguration.java` with similar timeouts. Add configurable timeout properties in `application.yml` under a `petclinic.client` prefix.

---

### 1.6 Fix Wildcard Path Patterns (Gap 5.4)
**Severity:** Medium | **Effort:** Small

Replace wildcards with explicit path parameters.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, replace all wildcard `*` path patterns with explicit `{ownerId}` parameters in `PetResource.java` (customers-service) and `VisitResource.java` (visits-service). Specifically, change `/owners/*/pets/{petId}` to `/owners/{ownerId}/pets/{petId}` and `/owners/*/pets/{petId}/visits` to `/owners/{ownerId}/pets/{petId}/visits`. Update the corresponding gateway route configuration in `api-gateway/application.yml` if needed. Update all existing tests that reference these paths.

---

### 1.7 Fix GenAI Service groupId (Gap 1.4)
**Severity:** Low | **Effort:** Small

Correct the copy-paste error in the GenAI service POM.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, in `spring-petclinic-genai-service/pom.xml`, change the `<groupId>` from `org.springframework.samples.petclinic.vets` to `org.springframework.samples.petclinic.genai`. Rebuild with `./mvnw clean install -DskipTests` to verify.

---

### 1.8 Add Prometheus Scrape Targets (Gap 6.3)
**Severity:** Medium | **Effort:** Small

Add missing services to Prometheus configuration.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, update `docker/prometheus/prometheus.yml` to add scrape targets for `genai-service` (port 8084) and `admin-server` (port 9090) following the same pattern as the existing service configurations. Both should use `/actuator/prometheus` as the metrics path.

---

### 1.9 Add GenAI Error Response Codes (Gap 2.3)
**Severity:** Medium | **Effort:** Small

Return proper HTTP status codes from the chat endpoint.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, in `spring-petclinic-genai-service/src/main/java/.../genai/PetclinicChatClient.java`, update the `exchange()` method to return `ResponseEntity<String>` instead of `String`. On success, return 200 OK. On exception, return 503 Service Unavailable with a structured error JSON body. Add a `@ControllerAdvice` for the GenAI service as well.

---

### 1.10 Vector Store Startup Fallback (Gap 7.5)
**Severity:** Medium | **Effort:** Small

Add graceful degradation when vets-service is unavailable during startup.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, in `spring-petclinic-genai-service/src/main/java/.../genai/VectorStoreController.java`, wrap the `webClient` call to vets-service in a try-catch. If the call fails (e.g., `WebClientResponseException`, `ConnectException`), log a warning and continue with an empty vector store rather than crashing. Add a retry mechanism with 3 attempts and 5-second delay using `Mono.retryWhen()`.

---

## Phase 2: Important (3–6 weeks)

### 2.1 Add Authentication and Authorization (Gap 4.1)
**Severity:** Critical | **Effort:** Large

Add Spring Security to protect endpoints.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, add Spring Security with the following approach: (1) Add `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server` to all domain services and the API gateway. (2) Configure the API gateway as an OAuth2 resource server. (3) Protect mutating endpoints (POST, PUT, DELETE) with authentication. (4) Allow GET endpoints for read-only access without authentication. (5) Ensure actuator health and prometheus endpoints remain publicly accessible. (6) Add security integration tests. Use only open-source libraries — do not use proprietary auth providers.

---

### 2.2 Increase Unit Test Coverage (Gap 3.1)
**Severity:** Critical | **Effort:** Large

Add comprehensive unit tests for all services.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, add unit tests to achieve >80% coverage for each domain service. Specifically: (1) In customers-service, add `OwnerResourceTest` with tests for create, findAll, findById (found and not-found), update; add tests for `PetResource` covering create, update, and pet type listing; add repository tests. (2) In vets-service, add tests for the VetResource caching behavior. (3) In visits-service, add tests for visit creation and single-pet visit retrieval. Use MockMvc, Mockito, and @WebMvcTest. Do not modify existing tests.

---

### 2.3 Add Pagination to List Endpoints (Gap 5.2)
**Severity:** High | **Effort:** Medium

Implement pagination for all list endpoints.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, add pagination support to `OwnerResource.findAll()`, `VetResource.showResourcesVetList()`, and `VisitResource.read()` by accepting `Pageable` parameters. Return `Page<T>` objects that include total count, page number, and page size metadata. Update the API gateway's `OwnerDetails` aggregation to pass pagination parameters through. Update existing tests. Default page size should be 20.

---

### 2.4 Add Circuit Breakers to GenAI Service (Gap 7.1)
**Severity:** High | **Effort:** Medium

Protect inter-service calls in GenAI with circuit breakers.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, add Resilience4j circuit breaker support to the GenAI service's `AIDataProvider`. Wrap calls to customers-service (`getAllOwners`, `addOwnerToPetclinic`, `addPetToOwner`) with circuit breakers. Configure: failure-rate-threshold=50, wait-duration-in-open-state=30s, sliding-window-size=10. Add fallback methods that return meaningful error messages. Add the resilience4j configuration in the genai-service `application.yml`.

---

### 2.5 Add Chat Input Validation (Gap 4.4)
**Severity:** High | **Effort:** Medium

Sanitize and validate chat input before passing to LLM.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, in the GenAI service, add input validation for the chat endpoint: (1) Limit input length to 2000 characters. (2) Reject empty or blank inputs with 400 Bad Request. (3) Add a configurable input sanitizer that strips HTML/script tags. (4) Add rate limiting using Bucket4j or a simple in-memory counter (max 30 requests per minute per client). (5) Add unit tests for all validation scenarios. Use only open-source libraries.

---

### 2.6 Add GenAI Service Tests (Gap 3.3)
**Severity:** High | **Effort:** Medium

Create comprehensive tests for the GenAI service.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, add tests for the GenAI service: (1) `PetclinicChatClientTest` — mock the `ChatClient` and verify prompt construction and error handling. (2) `PetclinicToolsTest` — mock `AIDataProvider` and verify each tool function. (3) `AIDataProviderTest` — use MockWebServer to test REST calls to customers-service and vector store operations. (4) `VectorStoreControllerTest` — test startup initialization with and without pre-embedded data. Use Spring Boot Test, Mockito, and MockWebServer.

---

### 2.7 Add API Versioning (Gap 5.1)
**Severity:** Medium | **Effort:** Medium

Add version prefixes to all API endpoints.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, add API versioning using URL prefix strategy. (1) Prefix all domain service endpoints with `/v1/` (e.g., `/v1/owners`, `/v1/vets`, `/v1/owners/*/pets/{petId}/visits`). (2) Update the API gateway routes to strip the correct number of prefixes. (3) Update the GenAI service's `AIDataProvider` to use the new versioned paths. (4) Update all tests. (5) Add a note in README about the versioning strategy.

---

### 2.8 Standardize Logging (Gap 6.1)
**Severity:** Medium | **Effort:** Medium

Adopt structured logging across all services.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, switch to structured JSON logging: (1) Add `logstash-logback-encoder` dependency to the root POM. (2) Create a shared `logback-spring.xml` configuration in each service that outputs JSON in production profile and plain text in development. (3) Add consistent log statements to all controller methods (info for mutations, debug for reads). (4) Include correlation/trace ID in all log entries leveraging Micrometer's trace context propagation. Use only the open-source logstash-logback-encoder library.

---

### 2.9 Add Custom Health Indicators (Gap 6.2)
**Severity:** Medium | **Effort:** Small

Add meaningful health checks for external dependencies.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, add custom `HealthIndicator` implementations: (1) In the GenAI service, add an `OpenAIHealthIndicator` that checks connectivity to the OpenAI API (or returns DOWN if the key is not configured). (2) In each domain service, add a `DatabaseHealthIndicator` that runs a simple query to verify DB connectivity. (3) In the API gateway, add a `DiscoveryHealthIndicator` that verifies Eureka connectivity. Register all indicators as Spring beans.

---

### 2.10 Add Secrets Management (Gap 4.3)
**Severity:** Medium | **Effort:** Medium

Replace raw environment variable secrets with a vault solution.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, add Spring Cloud Vault integration for secrets management: (1) Add `spring-cloud-starter-vault-config` dependency to the GenAI service. (2) Configure Vault connection properties in `bootstrap.yml`. (3) Move `OPENAI_API_KEY` to Vault's KV store. (4) Update the `application.yml` to reference Vault properties instead of environment variables. (5) Document the Vault setup in the README. (6) Keep environment variable fallback for local development.

---

## Phase 3: Polish (6–12 weeks)

### 3.1 Extract Shared DTO Library (Gap 1.1)
**Severity:** Medium | **Effort:** Medium

Create a common module for shared DTOs.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, create a new Maven module `spring-petclinic-common` under the root POM. Move shared DTO classes (`OwnerDetails`, `PetDetails`, `PetType`, `VisitDetails`, `PetRequest`) into this module under `org.springframework.samples.petclinic.common.dto`. Update the API gateway, GenAI service, and domain services to depend on this common module instead of maintaining their own copies. Remove the duplicated DTO classes. Rebuild and run all tests.

---

### 3.2 Add Integration / Contract Tests (Gap 3.2)
**Severity:** High | **Effort:** Large

Add cross-service integration and contract tests.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, add contract tests using Spring Cloud Contract: (1) Define producer contracts in the customers-service for `/owners` and `/owners/{id}/pets` endpoints. (2) Define producer contracts in the visits-service for `/pets/visits` endpoint. (3) Generate consumer stubs and add consumer-side tests in the API gateway. (4) Add an integration test module `spring-petclinic-integration-tests` that starts services with Testcontainers and verifies the full owner-details aggregation flow. Use only open-source testing libraries.

---

### 3.3 Introduce Service Layer (Gap 1.3)
**Severity:** Medium | **Effort:** Medium

Add a service layer between controllers and repositories.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, introduce a service layer in each domain service: (1) In customers-service, create `OwnerService` and `PetService` classes that encapsulate repository calls and business logic. Move logic from `OwnerResource` and `PetResource` into these services. (2) In vets-service, create `VetService`. (3) In visits-service, create `VisitService`. (4) Update controllers to delegate to service classes. (5) Update existing tests to mock service classes instead of repositories. (6) Add unit tests for the new service classes.

---

### 3.4 Add Retry Policies to Domain Services (Gap 7.3)
**Severity:** Medium | **Effort:** Medium

Add retry with exponential backoff for transient failures.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, add Resilience4j retry policies: (1) Add `resilience4j-spring-boot3` dependency to customers-service, vets-service, and visits-service. (2) Configure retry for database operations with 3 max attempts, 1s initial delay, and exponential backoff. (3) Add retry for the GenAI service's RestClient calls (3 attempts, 2s delay). (4) Configure via `application.yml` properties. (5) Add Micrometer metrics for retry events. (6) Add tests verifying retry behavior using WireMock.

---

### 3.5 Add Idempotency for POST Endpoints (Gap 7.4)
**Severity:** Medium | **Effort:** Medium

Prevent duplicate record creation on retries.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, add idempotency support for POST endpoints: (1) Create an `IdempotencyFilter` that reads an `Idempotency-Key` header from requests. (2) Store processed keys in a cache (Caffeine, 1-hour TTL) mapping to their responses. (3) If a duplicate key is received, return the cached response instead of creating a new record. (4) Apply the filter to `POST /owners`, `POST /owners/{id}/pets`, and `POST /owners/*/pets/{petId}/visits`. (5) Add unit tests. (6) Document the idempotency header in the OpenAPI configuration.

---

### 3.6 Add Alerting Rules (Gap 6.4)
**Severity:** Medium | **Effort:** Medium

Configure Prometheus alerting and Grafana notifications.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, add Prometheus alerting: (1) Create `docker/prometheus/alerts.yml` with rules for: error rate > 5% over 5 minutes, p99 latency > 2s, service instance down for > 1 minute, JVM heap usage > 80%. (2) Update `prometheus.yml` to load the alerting rules. (3) Add an Alertmanager container to `docker-compose.yml`. (4) Create a Grafana notification channel configuration. (5) Add a Grafana dashboard panel that shows active alerts.

---

### 3.7 Standardize Package Naming (Gap 1.2)
**Severity:** Low | **Effort:** Medium

Unify package structure across all services.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, standardize the package structure across all services to follow: `org.springframework.samples.petclinic.{service}.model` for entities, `org.springframework.samples.petclinic.{service}.repository` for repositories, `org.springframework.samples.petclinic.{service}.service` for business logic, `org.springframework.samples.petclinic.{service}.web` for controllers, `org.springframework.samples.petclinic.{service}.config` for configuration. Refactor the GenAI service which currently has all classes at the root package. Update all imports and tests.

---

### 3.8 Add CORS Configuration (Gap 4.5)
**Severity:** Low | **Effort:** Small

Explicitly configure CORS on the API Gateway.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, add CORS configuration to the API gateway's `application.yml` under `spring.cloud.gateway`. Allow origins `http://localhost:8080` and `http://localhost:4200` (for potential Angular CLI dev server). Allow methods GET, POST, PUT, DELETE, OPTIONS. Allow headers Content-Type, Authorization. Set max-age to 3600. Add a CORS integration test.

---

### 3.9 Standardize Response Wrapping (Gap 5.5)
**Severity:** Low | **Effort:** Small

Use consistent response envelopes across all endpoints.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, create a generic response wrapper record `ApiResponse<T>` in the shared common module with fields: `data` (the payload), `metadata` (optional, for pagination info). Update all list endpoints across customers-service, vets-service, and visits-service to return `ApiResponse<List<T>>` instead of raw lists. Update the API gateway DTO and aggregation logic to use the new wrapper. Update all tests.

---

### 3.10 Add Visit Validation (Gap 2.4)
**Severity:** Medium | **Effort:** Small

Add proper validation constraints to the Visit entity.

**Devin prompt:**
> In the repo `app-petclinic-microservices`, improve Visit validation: (1) Create a `VisitRequest` record with fields: `date` (`@NotNull`, `@PastOrPresent`), `description` (`@NotBlank`, `@Size(max=8192)`). (2) Update `VisitResource.create()` to accept `VisitRequest` instead of the `Visit` entity directly. (3) Map `VisitRequest` to `Visit` entity in the controller. (4) Add validation tests for invalid dates and missing descriptions. (5) Do not modify the existing `VisitResourceTest`.
