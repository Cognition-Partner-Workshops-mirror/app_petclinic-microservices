# Spring PetClinic Microservices — Gap Analysis

This document evaluates the codebase against seven engineering best-practice categories. Each identified gap is rated by **Severity** (Critical / High / Medium / Low) and estimated **Effort** (Small / Medium / Large).

---

## 1. Code Organization

### 1.1 GAP: No Shared Library for Common DTOs

**Current state:** DTO classes (`OwnerDetails`, `PetDetails`, `PetType`, `VisitDetails`, `PetRequest`, etc.) are duplicated across the API gateway, GenAI service, and domain services. For example, `OwnerDetails` exists in both `api-gateway/dto` and `genai-service/dto` with different shapes.

**Best practice:** Extract shared DTOs into a common library module (e.g., `spring-petclinic-common`) to avoid drift and duplication.

| Severity | Effort |
|---|---|
| Medium | Medium |

### 1.2 GAP: Inconsistent Package Naming Conventions

**Current state:** Domain services use `web` for controllers (e.g., `customers.web.OwnerResource`) while the API gateway uses `boundary.web` and `application`. GenAI service places controllers, tools, and configuration all at the root package level.

**Best practice:** Standardize on a consistent package structure across all services (e.g., `web`/`service`/`model`/`config`).

| Severity | Effort |
|---|---|
| Low | Medium |

### 1.3 GAP: Missing Service Layer in Domain Services

**Current state:** Controllers (`OwnerResource`, `PetResource`, `VisitResource`, `VetResource`) call repositories directly. There is no dedicated service layer to encapsulate business logic, making controllers responsible for data access, validation, and orchestration.

**Best practice:** Introduce a service layer between controllers and repositories to improve testability, reusability, and separation of concerns.

| Severity | Effort |
|---|---|
| Medium | Medium |

### 1.4 GAP: GenAI Service groupId Mismatch

**Current state:** The `spring-petclinic-genai-service` POM uses `groupId: org.springframework.samples.petclinic.vets` — clearly a copy-paste error from the vets service.

**Best practice:** Each module should have a correct, descriptive groupId (e.g., `org.springframework.samples.petclinic.genai`).

| Severity | Effort |
|---|---|
| Low | Small |

---

## 2. Error Handling

### 2.1 GAP: No Centralized Exception Handler

**Current state:** Only `ResourceNotFoundException` exists (annotated with `@ResponseStatus(404)`) in the customers-service. There is no `@ControllerAdvice` or global exception handler in any service. Validation errors, unexpected exceptions, and data access errors return default Spring Boot error responses with inconsistent formats.

**Best practice:** Each service should have a `@ControllerAdvice` that catches common exceptions and returns a standardized error response body (e.g., `{ "timestamp", "status", "error", "message", "path" }`).

| Severity | Effort |
|---|---|
| High | Small |

### 2.2 GAP: `findOwner` Returns `Optional` Directly

**Current state:** `OwnerResource.findOwner()` returns `Optional<Owner>`. When the owner is not found, this serializes to `null` with a 200 OK status instead of a proper 404 response.

**Best practice:** Unwrap the `Optional` and throw `ResourceNotFoundException` for missing resources, returning a 404 status.

| Severity | Effort |
|---|---|
| High | Small |

### 2.3 GAP: GenAI Service Catches All Exceptions as Generic String

**Current state:** `PetclinicChatClient.exchange()` catches `Exception` and returns a generic string `"Chat is currently unavailable."` with a 200 OK status. The error is logged but the client receives no indication that an error occurred at the HTTP level.

**Best practice:** Return appropriate HTTP error status codes (e.g., 503 Service Unavailable) and structured error responses. Distinguish between transient errors (retry-able) and permanent errors.

| Severity | Effort |
|---|---|
| Medium | Small |

### 2.4 GAP: No Validation on Visit Creation

**Current state:** `VisitResource.create()` uses `@Valid` on `Visit` but `Visit` entity only has `@Size(max=8192)` on description. No validation on `date` (can be null or past/future) and `petId` is overwritten from path variable anyway.

**Best practice:** Add meaningful validation constraints on the Visit request, and validate that the referenced petId actually exists (cross-service validation).

| Severity | Effort |
|---|---|
| Medium | Small |

---

## 3. Testing

### 3.1 GAP: Minimal Test Coverage

**Current state:** Only 6 test files exist across the entire project:
- `ApiGatewayControllerTest` (2 tests — circuit breaker fallback)
- `VisitsServiceClientIntegrationTest` (1 test — MockWebServer)
- `PetResourceTest` (1 test — GET pet)
- `VetResourceTest` (1 test — GET vets)
- `VisitResourceTest` (1 test — batch GET visits)
- `PetclinicConfigServerApplicationTests` (context load)
- `DiscoveryServerApplicationTests` (context load)
- `ApiGatewayApplicationTests` (context load)

No tests for: owner CRUD, pet creation/update, visit creation, GenAI service, error scenarios, validation failures, or data layer.

**Best practice:** Aim for >80% line coverage on business logic. Include unit tests for all controller endpoints, service layer, repository queries, and edge cases (validation failures, not-found, duplicate data).

| Severity | Effort |
|---|---|
| Critical | Large |

### 3.2 GAP: No Integration or End-to-End Tests

**Current state:** No integration tests that verify cross-service communication (e.g., gateway → customers → visits aggregation). No contract tests (e.g., Spring Cloud Contract or Pact) to verify API compatibility between services.

**Best practice:** Add contract tests for inter-service APIs and integration tests for critical flows.

| Severity | Effort |
|---|---|
| High | Large |

### 3.3 GAP: No Tests for GenAI Service

**Current state:** The `spring-petclinic-genai-service` has zero test files. The AI chat client, tool functions, data provider, and vector store controller are entirely untested.

**Best practice:** Add unit tests with mocked ChatClient and RestClient to verify tool invocations, error handling, and response formatting.

| Severity | Effort |
|---|---|
| High | Medium |

---

## 4. Security

### 4.1 GAP: No Authentication or Authorization

**Current state:** All endpoints are publicly accessible. There is no Spring Security dependency, no authentication mechanism, no role-based access control. `management.security.enabled: false` is explicitly set in shared config. All actuator endpoints are exposed (`management.endpoints.web.exposure.include: "*"`).

**Best practice:** Add Spring Security with at least basic authentication for management endpoints. Protect actuator endpoints (especially `/env`, `/configprops`, `/heapdump`) in production. Implement OAuth2/JWT for API endpoints.

| Severity | Effort |
|---|---|
| Critical | Large |

### 4.2 GAP: Actuator Endpoints Fully Exposed

**Current state:** All actuator endpoints are exposed without protection via `management.endpoints.web.exposure.include: "*"`. This includes sensitive endpoints like `/env`, `/configprops`, `/heapdump`, `/threaddump`, and `/jolokia`.

**Best practice:** Expose only necessary actuator endpoints (`/health`, `/info`, `/prometheus`) publicly. Protect or disable sensitive endpoints.

| Severity | Effort |
|---|---|
| Critical | Small |

### 4.3 GAP: API Key in Environment Variable Without Rotation

**Current state:** `OPENAI_API_KEY` is passed directly via environment variable and referenced in `application.yml` with a default fallback of `demo`. No secrets management, no rotation mechanism, no vault integration.

**Best practice:** Use a secrets management solution (e.g., HashiCorp Vault, Spring Cloud Vault) for API keys. Remove default fallback values for production secrets.

| Severity | Effort |
|---|---|
| Medium | Medium |

### 4.4 GAP: No Input Sanitization on Chat Endpoint

**Current state:** `PetclinicChatClient.exchange()` accepts raw `String` input and passes it directly to the LLM. No input validation, length limits, or prompt injection protection.

**Best practice:** Validate and sanitize chat input (length limits, content filtering). Consider prompt injection guards.

| Severity | Effort |
|---|---|
| High | Medium |

### 4.5 GAP: No CORS Configuration

**Current state:** No explicit CORS configuration is present. The AngularJS frontend is served from the same gateway, but if external clients need to access the API, CORS will be an issue.

**Best practice:** Explicitly configure CORS policies on the API gateway, restricting allowed origins, methods, and headers.

| Severity | Effort |
|---|---|
| Low | Small |

---

## 5. API Design

### 5.1 GAP: No API Versioning

**Current state:** API paths have no version prefix (e.g., `/owners`, `/vets`, `/pets/visits`). Breaking changes to the API would affect all clients immediately.

**Best practice:** Version APIs via URL prefix (`/api/v1/owners`) or header-based versioning.

| Severity | Effort |
|---|---|
| Medium | Medium |

### 5.2 GAP: No Pagination or Filtering

**Current state:** `OwnerResource.findAll()` and `VetResource.showResourcesVetList()` return all records without pagination. As data grows, these endpoints will become performance bottlenecks.

**Best practice:** Implement pagination (`page`, `size`, `sort` parameters) using Spring Data's `Pageable`. Add filtering capabilities for search queries.

| Severity | Effort |
|---|---|
| High | Medium |

### 5.3 GAP: No OpenAPI / Swagger Documentation

**Current state:** No `springdoc-openapi` or similar dependency exists. There are no API documentation endpoints (`/swagger-ui`, `/v3/api-docs`). Developers must read source code to understand the API.

**Best practice:** Add `springdoc-openapi-starter-webmvc-ui` (and `webflux-ui` for gateway) to auto-generate interactive API documentation.

| Severity | Effort |
|---|---|
| High | Small |

### 5.4 GAP: Wildcard Path Patterns

**Current state:** Several endpoints use wildcard `*` in path patterns (e.g., `/owners/*/pets/{petId}`, `/owners/*/pets/{petId}/visits`). This makes routing ambiguous and could match unintended paths.

**Best practice:** Use explicit path parameters (`/owners/{ownerId}/pets/{petId}`) for clarity and proper parameter binding.

| Severity | Effort |
|---|---|
| Medium | Small |

### 5.5 GAP: Inconsistent Response Wrapping

**Current state:** Some endpoints return raw entities (`List<Owner>`, `List<Vet>`), others use wrapper records (`Visits` with `items` field). The visits batch endpoint returns `{ "items": [...] }` but single-pet visits returns a raw list.

**Best practice:** Standardize response format. Use a consistent envelope (e.g., `{ "data": [...], "metadata": {...} }`) or consistently return raw collections.

| Severity | Effort |
|---|---|
| Low | Small |

---

## 6. Observability

### 6.1 GAP: Inconsistent Logging Patterns

**Current state:** Some controllers log operations (`log.info("Saving owner {}", ownerModel)`) while others have no logging at all (`VetResource` has zero log statements). No structured logging format (JSON). GenAI service logs at DEBUG level for AI advisors but INFO for errors.

**Best practice:** Adopt structured logging (JSON format) across all services. Log all mutating operations consistently. Include correlation IDs in log messages.

| Severity | Effort |
|---|---|
| Medium | Medium |

### 6.2 GAP: No Custom Health Indicators

**Current state:** Services rely solely on default Spring Boot health checks. No custom health indicators for database connectivity, Eureka registration status, or external service availability (e.g., OpenAI API).

**Best practice:** Add custom `HealthIndicator` implementations for critical dependencies (database, Eureka, OpenAI) to provide meaningful health status.

| Severity | Effort |
|---|---|
| Medium | Small |

### 6.3 GAP: Prometheus Does Not Scrape GenAI or Admin Services

**Current state:** `prometheus.yml` only scrapes 4 services (api-gateway, customers, visits, vets). The genai-service and admin-server are not included.

**Best practice:** Add all services to the Prometheus scrape configuration for complete observability.

| Severity | Effort |
|---|---|
| Medium | Small |

### 6.4 GAP: No Alerting Rules

**Current state:** Prometheus and Grafana are configured for metrics collection and visualization but no alerting rules are defined. There are no alerts for high error rates, latency spikes, or service unavailability.

**Best practice:** Define Prometheus alerting rules and configure Grafana alert channels for critical metrics (error rate > threshold, p99 latency, service down).

| Severity | Effort |
|---|---|
| Medium | Medium |

---

## 7. Resilience

### 7.1 GAP: Circuit Breaker Only on Gateway

**Current state:** Resilience4j circuit breaker is configured only in the API gateway (for `getOwnerDetails` and the genai route). Domain services that call each other (e.g., GenAI → customers-service) have no circuit breaker, retry, or timeout protection.

**Best practice:** Add circuit breaker and timeout configuration to all inter-service calls, especially in the GenAI service's `AIDataProvider` which makes synchronous calls to customers-service via `RestClient`.

| Severity | Effort |
|---|---|
| High | Medium |

### 7.2 GAP: No Timeout Configuration on RestClient Calls

**Current state:** `AIDataProvider` uses `RestClient.builder().build()` with default settings — no connection timeout, read timeout, or call timeout configured. A slow or unresponsive customers-service could block the GenAI service indefinitely.

**Best practice:** Configure explicit timeouts on all HTTP clients (connection timeout, read timeout, overall call timeout).

| Severity | Effort |
|---|---|
| High | Small |

### 7.3 GAP: No Retry Policy on Domain Services

**Current state:** The gateway has a `Retry` filter (1 retry for POST on SERVICE_UNAVAILABLE), but domain services have no retry logic for database operations or inter-service calls.

**Best practice:** Add retry policies with exponential backoff for transient failures (database connection issues, service unavailability).

| Severity | Effort |
|---|---|
| Medium | Medium |

### 7.4 GAP: No Idempotency on POST Endpoints

**Current state:** `POST /owners`, `POST /owners/{id}/pets`, and `POST /owners/*/pets/{petId}/visits` have no idempotency mechanism. Retrying a failed POST could create duplicate records.

**Best practice:** Implement idempotency keys (e.g., `Idempotency-Key` header) or use natural key-based deduplication for create operations.

| Severity | Effort |
|---|---|
| Medium | Medium |

### 7.5 GAP: Vector Store Has No Fallback

**Current state:** `VectorStoreController.loadVetDataToVectorStoreOnStartup()` fetches vet data from the vets-service at startup. If the vets-service is unavailable, the vector store initialization fails. The fallback only works if a pre-embedded `vectorstore.json` exists on the classpath.

**Best practice:** Add retry logic and graceful degradation for vector store initialization. Log a warning and continue with empty vector store if the vets-service is unavailable.

| Severity | Effort |
|---|---|
| Medium | Small |

---

## Summary Table

| # | Gap | Category | Severity | Effort |
|---|---|---|---|---|
| 1.1 | No shared library for common DTOs | Code Organization | Medium | Medium |
| 1.2 | Inconsistent package naming | Code Organization | Low | Medium |
| 1.3 | Missing service layer | Code Organization | Medium | Medium |
| 1.4 | GenAI groupId mismatch | Code Organization | Low | Small |
| 2.1 | No centralized exception handler | Error Handling | High | Small |
| 2.2 | `findOwner` returns Optional directly | Error Handling | High | Small |
| 2.3 | GenAI catches all exceptions as 200 OK | Error Handling | Medium | Small |
| 2.4 | No validation on Visit creation | Error Handling | Medium | Small |
| 3.1 | Minimal test coverage | Testing | Critical | Large |
| 3.2 | No integration or E2E tests | Testing | High | Large |
| 3.3 | No tests for GenAI service | Testing | High | Medium |
| 4.1 | No authentication or authorization | Security | Critical | Large |
| 4.2 | Actuator endpoints fully exposed | Security | Critical | Small |
| 4.3 | API key without secrets management | Security | Medium | Medium |
| 4.4 | No input sanitization on chat endpoint | Security | High | Medium |
| 4.5 | No CORS configuration | Security | Low | Small |
| 5.1 | No API versioning | API Design | Medium | Medium |
| 5.2 | No pagination or filtering | API Design | High | Medium |
| 5.3 | No OpenAPI documentation | API Design | High | Small |
| 5.4 | Wildcard path patterns | API Design | Medium | Small |
| 5.5 | Inconsistent response wrapping | API Design | Low | Small |
| 6.1 | Inconsistent logging patterns | Observability | Medium | Medium |
| 6.2 | No custom health indicators | Observability | Medium | Small |
| 6.3 | Prometheus missing services | Observability | Medium | Small |
| 6.4 | No alerting rules | Observability | Medium | Medium |
| 7.1 | Circuit breaker only on gateway | Resilience | High | Medium |
| 7.2 | No timeout on RestClient calls | Resilience | High | Small |
| 7.3 | No retry policy on domain services | Resilience | Medium | Medium |
| 7.4 | No idempotency on POST endpoints | Resilience | Medium | Medium |
| 7.5 | Vector store has no fallback | Resilience | Medium | Small |
