# Remediation Roadmap — Spring PetClinic Microservices Config

<!-- This roadmap prioritizes the gaps identified in GAP_ANALYSIS.md into three phases:
     Phase 1: Quick wins (high severity, low effort)
     Phase 2: Important improvements (high severity, medium effort)
     Phase 3: Polish (lower severity or higher effort) -->

---

## Phase 1 — Quick Wins

<!-- These items address critical or high-severity gaps with small effort.
     Estimated completion: 1-2 weeks. -->

### 1.1 Restrict Actuator Endpoint Exposure (SE-1)

**Gap**: All actuator endpoints are exposed without authentication, including sensitive endpoints
like `env`, `configprops`, `heapdump`, and `shutdown`.

**Action**: Limit exposed actuator endpoints in `application.yml` to only `health`, `info`,
`metrics`, and `prometheus`. Add Spring Security dependency to protect remaining endpoints.

**Devin Prompt**:
> In the `spring-petclinic-microservices-config` repo, edit `application.yml` to change
> `management.endpoints.web.exposure.include` from `"*"` to `health,info,metrics,prometheus`.
> Remove the line `management.security.enabled: false`. Add a comment explaining these are the
> safe endpoints for unauthenticated access.

---

### 1.2 Externalize Database Credentials (SE-2)

**Gap**: MySQL credentials (`root/petclinic`) are hardcoded in `application.yml`.

**Action**: Replace hardcoded values with environment variable references.

**Devin Prompt**:
> In the `spring-petclinic-microservices-config` repo, edit the `mysql` profile section of
> `application.yml`. Replace the hardcoded `username: root` with
> `username: ${MYSQL_USER:root}` and `password: petclinic` with
> `password: ${MYSQL_PASSWORD:petclinic}`. Add a comment noting these defaults are for
> development only and must be overridden in production.

---

### 1.3 Add Global Exception Handlers (EH-1, EH-2)

**Gap**: No centralized exception handling; inconsistent error response format.

**Action**: Add a `@ControllerAdvice` class to each domain service that returns RFC 7807
Problem Details responses.

**Devin Prompt**:
> In the `app-petclinic-microservices` repo, create a `GlobalExceptionHandler` class with
> `@ControllerAdvice` in each of the three domain services (customers, visits, vets) under their
> respective `web` package. Handle `ResourceNotFoundException` (404), `MethodArgumentNotValidException`
> (400 with field errors), and a catch-all `Exception` (500). Use Spring's `ProblemDetail` class
> for RFC 7807 responses. Add unit tests for each handler.

---

### 1.4 Fix `findOwner` to Return 404 (EH-4, AD-6)

**Gap**: `OwnerResource.findOwner` returns `Optional<Owner>` which serializes as `null` with
HTTP 200 when the owner is not found.

**Action**: Use `orElseThrow` to throw `ResourceNotFoundException`.

**Devin Prompt**:
> In the `app-petclinic-microservices` repo, edit `OwnerResource.findOwner` in
> `spring-petclinic-customers-service` to change the return type from `Optional<Owner>` to `Owner`
> and use `ownerRepository.findById(ownerId).orElseThrow(() -> new ResourceNotFoundException("Owner " + ownerId + " not found"))`.
> Update the corresponding test if one exists.

---

### 1.5 Add Structured Logging (OB-1, OB-2)

**Gap**: Default log format with no JSON structure or trace ID correlation.

**Action**: Add structured JSON logging for all services.

**Devin Prompt**:
> In the `spring-petclinic-microservices-config` repo, add the following to `application.yml`
> under the default profile:
> ```yaml
> logging:
>   pattern:
>     console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId:-},%X{spanId:-}] %-5level [%thread] %logger{36} - %msg%n"
> ```
> This adds trace and span ID correlation to all log output. Add a comment explaining the format.

---

### 1.6 Add HTTP Client Timeouts (RE-2)

**Gap**: No timeout configuration on HTTP clients; services can hang indefinitely.

**Action**: Configure connection and read timeouts in the shared config.

**Devin Prompt**:
> In the `spring-petclinic-microservices-config` repo, add the following to `application.yml`:
> ```yaml
> spring:
>   cloud:
>     discovery:
>       client:
>         simple:
>           instances:
>             connect-timeout: 3000
>             read-timeout: 5000
> ```
> Also add a comment recommending that each service's `WebClient` and `RestClient` beans
> configure explicit connect/read timeouts (3s/5s respectively).

---

### 1.7 Remove Deprecated Sleuth Property (OB-6)

**Gap**: `spring.sleuth.sampler.probability` is a deprecated Spring Cloud Sleuth property.

**Action**: Remove the obsolete property.

**Devin Prompt**:
> In the `spring-petclinic-microservices-config` repo, remove the `spring.sleuth.sampler.probability: 1.0`
> property from `application.yml`. The equivalent functionality is already provided by
> `management.tracing.sampling.probability: 1`. Add a comment on the existing tracing property
> noting it replaces the old Sleuth configuration.

---

### 1.8 Fix GenAI Service Eureka Discovery Safety (RE-7)

**Gap**: `AIDataProvider.getCustomerServiceUri()` calls `discoveryClient.getInstances(...).get(0)`
which throws `IndexOutOfBoundsException` if the service is not registered.

**Devin Prompt**:
> In the `app-petclinic-microservices` repo, edit `AIDataProvider.getCustomerServiceUri()` in
> `spring-petclinic-genai-service` to check if the instances list is empty before calling `get(0)`.
> If empty, throw a descriptive `IllegalStateException("customers-service not available in Eureka")`.
> Add a unit test verifying this behavior.

---

## Phase 2 — Important Improvements

<!-- These items address high-severity gaps with medium effort, or groups of medium-severity gaps.
     Estimated completion: 3-6 weeks. -->

### 2.1 Add Pagination and Filtering to APIs (AD-1)

**Gap**: List endpoints return unbounded result sets.

**Action**: Add `Pageable` support to Owner and Vet list endpoints.

**Devin Prompt**:
> In the `app-petclinic-microservices` repo, update `OwnerResource.findAll()` in the customers
> service to accept a `Pageable` parameter and return `Page<Owner>`. Update `VetResource.showResourcesVetList()`
> similarly. Update `OwnerRepository` and `VetRepository` to extend `PagingAndSortingRepository`.
> Add query parameters for filtering owners by `lastName`. Add tests for pagination and filtering.
> Update the API Gateway's client calls to pass pagination parameters through.

---

### 2.2 Add OpenAPI Documentation (AD-3)

**Gap**: No machine-readable API specification.

**Action**: Add `springdoc-openapi` to all services.

**Devin Prompt**:
> In the `app-petclinic-microservices` repo, add `springdoc-openapi-starter-webmvc-ui` dependency
> to the parent POM. Add `@Operation` and `@ApiResponse` annotations to all controller methods
> in customers-service, visits-service, and vets-service. Configure the API Gateway to aggregate
> OpenAPI specs from all services at `/swagger-ui.html`. Add a test verifying the OpenAPI spec
> is generated correctly.

---

### 2.3 Add Circuit Breakers to GenAI Service (RE-1)

**Gap**: GenAI service has no circuit breakers for its calls to Customers and Vets services.

**Action**: Wrap `AIDataProvider` calls with Resilience4j circuit breakers.

**Devin Prompt**:
> In the `app-petclinic-microservices` repo, add `spring-cloud-starter-circuitbreaker-resilience4j`
> to the `spring-petclinic-genai-service` POM. Wrap `getAllOwners()`, `addOwnerToPetclinic()`, and
> `addPetToOwner()` in `AIDataProvider` with `@CircuitBreaker` annotations. Define fallback methods
> that return meaningful error messages. Add circuit breaker configuration to
> `genai-service.yml` in the config repo with sensible defaults (failure rate threshold 50%,
> wait duration 30s, sliding window size 10).

---

### 2.4 Add Consumer-Driven Contract Tests (TE-3)

**Gap**: No contract tests between services.

**Action**: Implement Spring Cloud Contract tests.

**Devin Prompt**:
> In the `app-petclinic-microservices` repo, add `spring-cloud-starter-contract-verifier` to the
> customers-service and visits-service POMs. Create contract DSL files for the key API endpoints:
> `GET /owners`, `GET /owners/{id}`, `POST /owners`, `GET /vets`, `POST /owners/{id}/pets`,
> `GET /pets/visits`. Generate and run the verifier tests. In the API Gateway, add
> `spring-cloud-contract-stub-runner` tests that verify the gateway's client calls match the contracts.

---

### 2.5 Increase Unit Test Coverage (TE-1)

**Gap**: Minimal test coverage across services.

**Action**: Add tests for uncovered controller actions and edge cases.

**Devin Prompt**:
> In the `app-petclinic-microservices` repo, add the following WebMvcTest tests:
> 1. `OwnerResourceTest` — test `createOwner`, `findOwner` (found + not found), `updateOwner`
>    (found + not found), and `findAll` in the customers service.
> 2. Extend `PetResourceTest` — add tests for `processCreationForm`, `processUpdateForm`,
>    `getPetTypes`, and error cases (owner not found, pet not found).
> 3. Extend `VisitResourceTest` — add test for `create` endpoint and single-pet `read` endpoint.
> 4. `PetclinicChatClientTest` — mock the ChatClient and test the exchange endpoint including
>    error handling. Use `@WebFluxTest` since it's a reactive service.
> Run all tests and verify they pass.

---

### 2.6 Add Spring Security with Basic Auth (SE-3)

**Gap**: No authentication on any endpoint.

**Action**: Add Spring Security as a baseline.

**Devin Prompt**:
> In the `app-petclinic-microservices` repo, add `spring-boot-starter-security` to the parent POM.
> Create a `SecurityConfig` class in each domain service that permits all API endpoints but requires
> authentication for actuator endpoints (except `/actuator/health` and `/actuator/info`).
> Use HTTP Basic auth with credentials configurable via `spring.security.user.name` and
> `spring.security.user.password` in the config repo. Update existing tests to include
> `@WithMockUser` where needed. Add security configuration to `application.yml` in the config repo.

---

### 2.7 Configure Per-Environment Trace Sampling (OB-3)

**Gap**: 100% trace sampling in all environments creates overhead in production.

**Action**: Set production sampling to 10% while keeping 100% for development.

**Devin Prompt**:
> In the `spring-petclinic-microservices-config` repo, keep `management.tracing.sampling.probability: 1`
> in the default profile for development. Add a new `production` profile section to `application.yml`
> with `management.tracing.sampling.probability: 0.1`. Add a comment explaining the rationale:
> 100% in dev for debugging, 10% in production to balance observability with performance.

---

### 2.8 Add Retry and Bulkhead to Service Clients (RE-3, RE-4)

**Gap**: No retry logic or bulkheads on service-to-service calls.

**Devin Prompt**:
> In the `app-petclinic-microservices` repo, add Resilience4j retry and bulkhead annotations to
> `CustomersServiceClient` and `VisitsServiceClient` in the API Gateway. Configure retries with
> max 3 attempts, 500ms wait, exponential backoff. Configure thread pool bulkhead with max
> concurrent calls of 10. Add the corresponding Resilience4j configuration properties to
> `api-gateway.yml` in the config repo. Add tests verifying retry behavior using `@SpringBootTest`
> with WireMock.

---

## Phase 3 — Polish

<!-- These items address lower-severity gaps or require larger effort. They improve the system
     but are not urgent. Estimated completion: ongoing. -->

### 3.1 Create Shared API Contract Module (CO-1, CO-2)

**Gap**: DTOs duplicated across services.

**Devin Prompt**:
> In the `app-petclinic-microservices` repo, create a new Maven module `spring-petclinic-api-contracts`
> containing shared DTOs: `OwnerDetails`, `PetDetails`, `PetType`, `VisitDetails`, `Vet`, `Specialty`.
> Use Java records where possible. Add this module as a dependency to `api-gateway` and `genai-service`.
> Remove the duplicated DTO classes from those services and update imports. Run all tests to verify
> nothing breaks.

---

### 3.2 Add API Versioning (AD-2)

**Gap**: No API versioning strategy.

**Devin Prompt**:
> In the `app-petclinic-microservices` repo, add URL-based versioning to all REST controllers.
> Update `@RequestMapping` annotations to include `/v1/` prefix (e.g., `/v1/owners`, `/v1/vets`,
> `/v1/owners/{id}/pets`). Update the API Gateway route predicates in `api-gateway.yml` (in this
> config repo) to match the new paths. Update all tests and client calls. Add a `@Deprecated`
> marker to the unversioned endpoints for backward compatibility and document the versioning policy.

---

### 3.3 Add CORS Configuration (SE-5)

**Gap**: No explicit CORS policy.

**Devin Prompt**:
> In the `spring-petclinic-microservices-config` repo, add CORS configuration to `api-gateway.yml`:
> ```yaml
> spring:
>   cloud:
>     gateway:
>       globalcors:
>         cors-configurations:
>           '[/**]':
>             allowed-origins: "${CORS_ALLOWED_ORIGINS:http://localhost:8080}"
>             allowed-methods: GET,POST,PUT,DELETE,OPTIONS
>             allowed-headers: "*"
>             max-age: 3600
> ```
> Add a comment explaining the CORS policy and how to customize allowed origins per environment.

---

### 3.4 Add Configuration Validation Tests (TE-5)

**Gap**: No validation that YAML files in the config repo are syntactically correct.

**Devin Prompt**:
> In the `spring-petclinic-microservices-config` repo, add a GitHub Actions workflow
> (`.github/workflows/validate-config.yml`) that runs on every push and PR. The workflow should:
> 1. Use a YAML linter (e.g., `yamllint`) to validate all `.yml` files.
> 2. Use a custom script to verify that all expected service config files exist.
> 3. Optionally, start a Spring Cloud Config Server in `native` mode and verify each service's
>    config can be fetched at `/application/default`, `/customers-service/default`, etc.

---

### 3.5 Add Idempotency Keys to POST Endpoints (RE-5)

**Gap**: POST endpoints can create duplicate records on retry.

**Devin Prompt**:
> In the `app-petclinic-microservices` repo, add an `Idempotency-Key` header support to the
> `createOwner`, `processCreationForm` (pet), and `create` (visit) endpoints. Implement a simple
> in-memory idempotency store using Caffeine cache with a 10-minute TTL. If a duplicate key is
> received, return the original response with HTTP 200 instead of creating a new record. Add
> tests verifying idempotent behavior.

---

### 3.6 Add Custom Health Indicators (OB-5)

**Gap**: Only default health indicators are registered.

**Devin Prompt**:
> In the `app-petclinic-microservices` repo, add custom `HealthIndicator` implementations:
> 1. In customers-service: `DatabaseHealthIndicator` that runs a simple query.
> 2. In api-gateway: `EurekaHealthIndicator` that verifies Eureka connectivity and checks that
>    expected services are registered.
> 3. In genai-service: `OpenAIHealthIndicator` that checks LLM API availability.
> Register all health indicators as Spring beans. Add tests for each.

---

### 3.7 Set Up Alerting Rules (OB-4)

**Gap**: Prometheus monitoring exists but no alerting is configured.

**Devin Prompt**:
> In the `app-petclinic-microservices` repo, create a Prometheus alerting rules file at
> `docker/prometheus/alerts.yml` with rules for: high error rate (>5% 5xx in 5 min),
> high latency (p99 > 2s), service down (up == 0 for 1 min), high memory usage (>90%).
> Update the Prometheus config to load the alert rules. Add an Alertmanager service to
> `docker-compose.yml` configured to send alerts to a webhook (configurable).

---

### 3.8 Add Load Testing Suite (TE-6)

**Gap**: No performance tests.

**Devin Prompt**:
> In the `app-petclinic-microservices` repo, create a `load-tests/` directory with k6 scripts
> that test: `GET /api/customer/owners` (list owners), `GET /api/vet/vets` (list vets),
> `POST /api/customer/owners` (create owner), `GET /api/gateway/owners/1` (aggregation endpoint).
> Define two scenarios: smoke test (10 VUs, 30s) and stress test (100 VUs, 5 min). Include a
> `README.md` with instructions for running locally and in CI.

---

### 3.9 Add Environment-Based Config Directory Structure (CO-4)

**Gap**: All environment configuration is embedded in multi-document YAML files.

**Devin Prompt**:
> In the `spring-petclinic-microservices-config` repo, restructure the configuration to use
> profile-specific files: keep the base `application.yml` for shared defaults, create
> `application-docker.yml` for Docker overrides, `application-mysql.yml` for MySQL settings,
> and `application-production.yml` for production settings. Extract the corresponding `---`
> sections from the current files. Update the Config Server's search locations to include the
> new file structure. Test by starting the Config Server and verifying each profile resolves
> correctly.

---

## Priority Matrix

<!-- Quick reference mapping gap IDs to phases. -->

| Phase | Gap IDs | Theme |
|---|---|---|
| **Phase 1** | SE-1, SE-2, EH-1, EH-2, EH-4, AD-6, OB-1, OB-2, OB-6, RE-2, RE-7 | Security hardening, error standardization, observability baseline, critical resilience |
| **Phase 2** | AD-1, AD-3, RE-1, RE-3, RE-4, TE-1, TE-3, SE-3, OB-3, EH-3 | API maturity, test coverage, authentication, advanced resilience |
| **Phase 3** | CO-1, CO-2, CO-3, CO-4, AD-2, AD-4, AD-5, AD-7, SE-4, SE-5, SE-6, SE-7, TE-2, TE-4, TE-5, TE-6, OB-4, OB-5, RE-5, RE-6, RE-8 | Code quality, API polish, full security, comprehensive testing, production-readiness |
