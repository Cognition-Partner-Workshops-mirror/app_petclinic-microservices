# Gap Analysis — Spring PetClinic Microservices Config

<!-- This document evaluates the Spring PetClinic Microservices system against engineering
     best practices across seven categories. Gaps are rated by Severity and Effort. -->

## 1. Code Organization

### 1.1 Current State

- **Config repo**: Flat structure with one YAML per service plus a shared `application.yml`. Simple
  and easy to navigate.
- **Application repo**: Well-separated Maven modules per service following standard Spring Boot
  layouts (`model/`, `web/`, `system/`, `config/` packages).
- **Shared code**: No shared library module — DTO classes are duplicated across services (e.g.,
  `OwnerDetails`, `PetDetails`, `PetType` exist in both the API Gateway and GenAI service with
  different representations).

### 1.2 Identified Gaps

| # | Gap | Severity | Effort | Details |
|---|---|---|---|---|
| CO-1 | **Duplicated DTO classes across services** | Medium | Medium | `OwnerDetails`, `PetDetails`, `PetType`, `Vet`, `Specialty`, `VisitDetails` are copy-pasted between api-gateway, genai-service, and domain services. Changes must be synchronized manually. |
| CO-2 | **No shared contract/API module** | Medium | Medium | No shared Maven module for API contracts or DTOs. Each consumer defines its own version of the response types, risking drift. |
| CO-3 | **Inconsistent package naming** | Low | Small | API Gateway uses `boundary.web` and `application` packages while domain services use `web` and `model`. Minor inconsistency in conventions. |
| CO-4 | **Config repo lacks structure for multiple environments** | Low | Small | All profiles are embedded in the same YAML files using `---` separators. No directory-based environment separation (e.g., `dev/`, `staging/`, `prod/`). |

---

## 2. Error Handling

### 2.1 Current State

- `ResourceNotFoundException` with `@ResponseStatus(404)` is used in the Customers Service.
- The GenAI `PetclinicChatClient` catches generic `Exception` and returns a plaintext error string.
- The API Gateway `FallbackController` returns HTTP 503 with a plaintext body.
- No global `@ControllerAdvice` or standardized error response format.

### 2.2 Identified Gaps

| # | Gap | Severity | Effort | Details |
|---|---|---|---|---|
| EH-1 | **No centralized exception handler** | High | Small | No `@ControllerAdvice` in any service. Each endpoint handles errors independently, leading to inconsistent error responses. |
| EH-2 | **No standardized error response format** | High | Small | Errors return Spring Boot's default `WhitelabelErrorView` JSON or raw strings. No RFC 7807 Problem Details or custom error envelope. |
| EH-3 | **Catch-all `Exception` in GenAI chat** | Medium | Small | `PetclinicChatClient.exchange` catches `Exception` broadly, masking specific failure types. Returns a plaintext string rather than proper HTTP error. |
| EH-4 | **`findOwner` returns `Optional` directly** | Medium | Small | `OwnerResource.findOwner` returns `Optional<Owner>` to the client. When empty, Spring serializes `null` with HTTP 200 instead of 404. |
| EH-5 | **No validation error handling** | Medium | Small | `@Valid` on request bodies will throw `MethodArgumentNotValidException` with Spring's default format. No custom handler to produce user-friendly messages. |

---

## 3. Testing

### 3.1 Current State

- Three `@WebMvcTest` unit tests exist: `PetResourceTest`, `VetResourceTest`, `VisitResourceTest`.
- Two `ApplicationTests` classes for Config Server and Discovery Server (context loading only).
- One integration test: `VisitsServiceClientIntegrationTest` in the API Gateway.
- One circuit breaker test: `CircuitBreakerTest` in the API Gateway (inferred from file listing).
- **No tests in the config repo itself** (expected — it is pure configuration).

### 3.2 Identified Gaps

| # | Gap | Severity | Effort | Details |
|---|---|---|---|---|
| TE-1 | **Very low test coverage** | Critical | Large | Only 1 test per controller for 3 of 8 services. Owner creation/update, pet creation, visit creation, and all GenAI paths are untested. |
| TE-2 | **No integration/E2E tests** | High | Large | No tests verify cross-service communication (e.g., Gateway → Customers → Visits aggregation). Only one basic integration test for `VisitsServiceClient`. |
| TE-3 | **No contract tests** | High | Medium | No consumer-driven contract tests (e.g., Spring Cloud Contract or Pact) between services. API changes can silently break consumers. |
| TE-4 | **No GenAI service tests** | High | Medium | The GenAI module has zero tests. Chat flow, tool functions, vector store loading, and error handling are all untested. |
| TE-5 | **No configuration validation tests** | Medium | Small | No tests verify that YAML files in this config repo parse correctly or contain valid Spring configuration. |
| TE-6 | **No load/performance tests** | Low | Large | No JMeter, Gatling, or k6 performance tests to baseline throughput and latency. |

---

## 4. Security

### 4.1 Current State

- Actuator security is explicitly **disabled**: `management.security.enabled: false`.
- All actuator endpoints are exposed: `management.endpoints.web.exposure.include: "*"`.
- MySQL credentials are hardcoded in `application.yml` (`root/petclinic`).
- No authentication or authorization on any API endpoint.
- No CORS configuration.
- HTTPS is not configured.

### 4.2 Identified Gaps

| # | Gap | Severity | Effort | Details |
|---|---|---|---|---|
| SE-1 | **All actuator endpoints exposed without authentication** | Critical | Small | `management.endpoints.web.exposure.include: "*"` with `management.security.enabled: false` exposes `env`, `configprops`, `heapdump`, `shutdown` etc. to unauthenticated callers. |
| SE-2 | **Database credentials hardcoded in config** | Critical | Small | `application.yml` contains `username: root` and `password: petclinic` in plaintext. Should use encrypted values, Vault, or environment variables. |
| SE-3 | **No authentication/authorization on APIs** | High | Large | No Spring Security dependency. All endpoints are publicly accessible. No OAuth2, JWT, or basic auth. |
| SE-4 | **No input sanitization beyond Bean Validation** | Medium | Medium | Only `@NotBlank`, `@Digits`, `@Size`, and `@Min` validation. No XSS prevention, SQL injection hardening beyond JPA parameterized queries. |
| SE-5 | **No CORS configuration** | Medium | Small | API Gateway and domain services have no explicit CORS policy. Browser-based clients may face cross-origin issues, or the default permissive behavior may be unsafe. |
| SE-6 | **No HTTPS/TLS configuration** | Medium | Medium | All services communicate over plain HTTP. No TLS termination is configured at the application level. |
| SE-7 | **OpenAI API key fallback to `demo`** | Low | Small | `OPENAI_API_KEY` defaults to `demo` when not set, which could mask configuration errors in production. |

---

## 5. API Design

### 5.1 Current State

- RESTful resource naming (`/owners`, `/vets`, `/owners/{id}/pets`).
- Appropriate HTTP methods and status codes (201, 204, 200).
- JSON responses with `application/json` content type.
- Path-variable validation with `@Min(1)`.
- Micrometer `@Timed` annotations for request metrics.

### 5.2 Identified Gaps

| # | Gap | Severity | Effort | Details |
|---|---|---|---|---|
| AD-1 | **No pagination or filtering** | High | Medium | `findAll()` on Owners and Vets returns unbounded lists. No `Pageable` support, no search/filter parameters. |
| AD-2 | **No API versioning strategy** | Medium | Medium | No URL path version prefix (e.g., `/v1/`) or header-based versioning. Breaking changes would impact all consumers. |
| AD-3 | **No OpenAPI/Swagger documentation** | Medium | Medium | No `springdoc-openapi` dependency. API consumers have no machine-readable specification. |
| AD-4 | **Wildcard path patterns** | Medium | Small | `PUT /owners/*/pets/{petId}` uses a wildcard for ownerId, meaning the owner segment is ignored. Semantically misleading. |
| AD-5 | **Inconsistent response wrapping** | Low | Small | Visit batch endpoint returns `Visits` record wrapper, but other list endpoints return bare arrays. No consistent envelope pattern. |
| AD-6 | **`Optional<Owner>` returned as response body** | Medium | Small | Returns `null` with 200 instead of 404 when owner not found. |
| AD-7 | **No HATEOAS links** | Low | Medium | No hypermedia links in responses for discoverability. |

---

## 6. Observability

### 6.1 Current State

- Prometheus metrics enabled with Micrometer + Prometheus registry.
- Distributed tracing via Zipkin with 100% sampling.
- Grafana + Prometheus in Docker Compose.
- Spring Boot Actuator exposes health, info, metrics endpoints.
- `@Timed` annotations on controllers.
- JDBC query observation via `datasource-micrometer`.
- Spring Boot Admin for service monitoring.

### 6.2 Identified Gaps

| # | Gap | Severity | Effort | Details |
|---|---|---|---|---|
| OB-1 | **No structured logging** | High | Small | Uses default Spring Boot logging format. No JSON-structured logging for log aggregation tools (ELK, Loki). |
| OB-2 | **No log correlation with trace IDs** | Medium | Small | While tracing is configured, log format does not include trace/span IDs for correlating logs with distributed traces. |
| OB-3 | **100% trace sampling in all profiles** | Medium | Small | `sampling.probability: 1` is appropriate for dev but creates excessive overhead in production. No per-environment sampling configuration. |
| OB-4 | **No alerting rules** | Medium | Medium | Prometheus is set up but no alert rules or Alertmanager configuration exists. |
| OB-5 | **No custom health indicators** | Low | Small | Services rely on default health indicators. No custom checks for database connectivity, Eureka registration status, or Config Server availability. |
| OB-6 | **Deprecated `spring.sleuth` property** | Low | Small | `application.yml` still references `spring.sleuth.sampler.probability` which is a Spring Cloud Sleuth property. The project uses Micrometer Tracing, so this property is ignored. |

---

## 7. Resilience

### 7.1 Current State

- API Gateway has Resilience4j circuit breakers on the `getOwnerDetails` aggregation and GenAI route.
- Gateway retry filter for `SERVICE_UNAVAILABLE` responses (1 retry on POST).
- Graceful shutdown configured (`server.shutdown: graceful`).
- Fallback controller for GenAI service unavailability.
- Eureka `prefer-ip-address: true` to avoid DNS resolution issues.

### 7.2 Identified Gaps

| # | Gap | Severity | Effort | Details |
|---|---|---|---|---|
| RE-1 | **No circuit breakers on domain services** | High | Medium | Circuit breakers only exist in the API Gateway. Domain services making cross-service calls (GenAI → Customers, GenAI → Vets) have no circuit breaker protection. |
| RE-2 | **No timeout configuration** | High | Small | No explicit HTTP client timeouts or connection pool settings. Services could hang indefinitely on slow downstream calls. |
| RE-3 | **No retry policy on domain services** | Medium | Small | Retries only configured at the Gateway level for POST requests. No retry logic in `CustomersServiceClient`, `VisitsServiceClient`, or `AIDataProvider`. |
| RE-4 | **No bulkhead/rate limiting** | Medium | Medium | No thread pool bulkheads or rate limiters to prevent cascade failures or abuse. |
| RE-5 | **No idempotency mechanisms** | Medium | Medium | POST endpoints for creating owners, pets, and visits have no idempotency keys. Retries could create duplicate records. |
| RE-6 | **Eureka single instance** | Medium | Medium | Discovery Server runs in standalone mode with no replication. Single point of failure for service discovery. |
| RE-7 | **GenAI hard dependency on Customers Service** | Medium | Small | `AIDataProvider.getCustomerServiceUri()` calls `discoveryClient.getInstances(...).get(0)` — throws `IndexOutOfBoundsException` if the service is not registered. |
| RE-8 | **Cache only enabled in `production` profile** | Low | Small | `CacheConfig` is annotated with `@Profile("production")`. Default and Docker profiles have no caching, leading to repeated DB calls. |

---

## Summary Table

| Category | Critical | High | Medium | Low |
|---|---|---|---|---|
| **Code Organization** | 0 | 0 | 2 | 2 |
| **Error Handling** | 0 | 2 | 3 | 0 |
| **Testing** | 1 | 3 | 1 | 1 |
| **Security** | 2 | 1 | 3 | 1 |
| **API Design** | 0 | 1 | 3 | 3 |
| **Observability** | 0 | 1 | 2 | 3 |
| **Resilience** | 0 | 2 | 4 | 2 |
| **Total** | **3** | **10** | **18** | **12** |
