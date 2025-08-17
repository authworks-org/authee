# Authee

> Java-first HLD tailored for development teams: frameworks, module/package layout, class/interface sketches, build & deployment guidance.

---

## 1. Purpose

This document adapts the previously-approved Authee HLD to a Java (Spring Boot) implementation. It defines recommended frameworks, libraries, package/module layout, integration patterns, and operational guidance so engineering teams can start implementing features with predictable conventions.

## 2. Non-functional constraints

* Java 21 (LTS-compatible), Spring Boot (3.x+).
* Gradle as primary build tool (wrapper included). Maven support optional.
* Container-first (Docker) and Kubernetes-friendly artifacts.
* Strict tenancy isolation; no cross-tenant leakage.

## 3. Recommended Tech Stack

* **Framework**: Spring Boot 3.x, Spring Security, Spring Authorization Server (where useful).
* **SAML**: `spring-security-saml2-service-provider` + OpenSAML utilities.
* **JWT/JWK**: `nimbus-jose-jwt` for signing/verification; Spring Security JWT support.
* **Policy Engine**: custom DSL with Drools or a lightweight in-process evaluator (preferred custom for performance & versioning).
* **DB**: Postgres + Spring Data JPA (Hibernate) for strong consistency (config store, tenants, clients).
* **Cache**: Redis (Redisson client) for short-lived state, rate limits, device hints.
* **Queue/Streams**: Kafka (optional) or Rabbit for audit/event streaming.
* **Secrets/KMS**: AWS KMS / GCP KMS / HashiCorp Vault integration via Spring Cloud Vault or dedicated KMS client.
* **Observability**: OpenTelemetry Java agent + Micrometer (Prometheus) + Tempo/Jaeger for traces.
* **Logging**: Structured logs via Logback + Logstash encoder (JSON), correlation IDs.
* **Testing**: JUnit 5, Mockito, Testcontainers for Postgres/Redis/SAML idp stubs.
* **Build**: Gradle (Kotlin DSL) with `spring-boot` plugin; Docker image via `jib` or Dockerfile.

## 4. Project & Module Layout

Monorepo with multiple modules for clarity and isolated CI:

```
authee/
├─ build.gradle.kts
├─ settings.gradle.kts
├─ authee-core/           # Spring Boot app (exposes API)
├─ authee-protocols/      # oauth, saml handlers, endpoints
├─ authee-policy/         # policy DSL, evaluator, simulation
├─ authee-risk/           # risk scoring pipeline
├─ authee-tokens/         # token mint/verify, jwks management
├─ authee-claims/         # claims normalization, mappers
├─ authee-mfa/            # plugins for WebAuthn/TOTP/push
├─ authee-admin/          # admin APIs & minimal console
├─ authee-shared/         # common models, exceptions, utils
├─ libs/*                 # internal libraries (sdk-gen etc.)
└─ deploy/
```

* Each module is a Gradle subproject producing a JAR. `authee-core` depends on modules and is the executable Spring Boot image.

## 5. Key Components & Java Implementation Patterns

### 5.1 API Gateway

* Use API Gateway in infra (Ingress) for public routing. Inside the app, `spring-web` controllers expose endpoints.
* Implement request filters (Spring `OncePerRequestFilter`) to attach correlation IDs, basic auth for admin API, and rate-limiting hooks.

### 5.2 Auth Core

* `AuthOrchestrator` (singleton service) coordinates flows.
* Use Spring's Handler pattern: `ProtocolHandler` interface with implementations `OAuth2Handler`, `SAMLHandler`.

```java
public interface ProtocolHandler {
    AuthResponse handleAuthRequest(AuthRequest req);
}
```

* Use Reactor (Spring WebFlux) only for I/O-heavy endpoints if required; otherwise MVC is fine. Keep services async-ready.

### 5.3 Token Service

* `TokenService` issues JWTs; use `nimbus-jose-jwt` for signing with KMS-managed keys.
* Provide `TokenStore` interface that can be stateless (JWT) or opaque (Redis-backed) implementations.

### 5.4 Policy Engine

* Implement a small DSL parser (ANTLR or a custom parser). Policies are stored with versions.
* Provide `PolicyEvaluator` that returns `PolicyDecision { effect, obligations, reason }`.
* Include a dry-run simulator exposing differences.

### 5.5 Risk Engine

* Plug-in architecture for signals: `RiskSignalProvider` (IP, device fingerprint, ASN, failed attempts).
* Combine via pipeline to produce `RiskScore` (0-100) with reasons map.

### 5.6 MFA Orchestrator

* Core orchestrator that schedules step-up: interface `MfaProvider { begin(), verify(), cancel() }`.
* Implementations: WebAuthn (Yubico WebAuthn server libs), TOTP (Google Authenticator compatible), push (via provider SDK).

### 5.7 Claims Service

* Claims mapper configurable per tenant: `ClaimsMapper` interface, pluggable mapping strategies.
* Enforce data minimization: mapper reads requested scopes & purpose and emits filtered claim set.

### 5.8 Config Manager

* Store signed JSON/YAML bundles in Postgres (or Git-backed store). Use a `ConfigApplier` that validates and reloads with feature gates.
* Provide safe reload with health checks and canary.

### 5.9 Audit Logger

* `AuditService` writes append-only events to Kafka or directly to `Audit Index` (Elasticsearch/OpenSearch). Events are JSON and include correlationId, tenantId.

## 6. Data Model (JPA Entities) – examples

* `TenantEntity` (id, name, parentId, active, configRef)
* `ClientEntity` (id, tenantId, clientId, jwksUri, redirectUris, grantTypes)
* `KeysetEntity` (kid, tenantId, algorithm, publicKey, metadata)
* `PolicyEntity` (id, tenantId, dsl, version, mode)
* `AuditEventEntity` (id, tenantId, correlationId, eventType, payload, createdAt)

Use explicit DTOs and mappers (MapStruct) between JPA entities and API models.

## 7. APIs & Controllers

* Use Spring `@RestController` for all endpoints.
* Group endpoints: `/oauth2/*`, `/saml/*`, `/admin/*`.
* Use OpenAPI annotations (springdoc-openapi) to publish `openapi.yaml` and auto-generate SDKs.

## 8. Security Implementation

* Use Spring Security for endpoint protection; admin APIs require client credentials or mTLS.
* Implement per-tenant `JwtDecoder` lookup using `jwtDecoderRegistry` that maps tenant->jwks URI or local keyset.
* Protect CSRF for browser-based flows; ensure state/nonce validation.
* Key rotation: maintain multiple `SigningKey` with `status` (`ACTIVE`, `PREVIOUS`, `REVOKED`) and expose JWKS aggregating active+previous for smooth transition.

## 9. Observability & Tracing

* Integrate OpenTelemetry SDK + auto-instrumentation.
* Ensure each incoming request creates/propagates `traceparent` and `correlation-id`.
* Expose metrics via Micrometer Prometheus registry. Add dashboards for auth success/failure, policy latency, MFA prompt rates.

## 10. Testing Strategy

* Unit tests: JUnit 5 + Mockito
* Integration tests: Testcontainers for Postgres + Redis, embedded SAML IdP (e.g., OpenSAML test harness), or a stub IdP container.
* Contract tests: verify OpenAPI client-server compatibility.
* Security tests: OWASP ZAP in CI; dependency scans.

## 11. Build & Local Development

* Provide `gradlew` wrapper. Key tasks:

    * `./gradlew build` — compile/test
    * `./gradlew :authee-core:bootRun` — run locally
    * `./gradlew jibDockerBuild` — build container (or `docker build` with Dockerfile)

## 12. Dockerfile (example)

```dockerfile
FROM eclipse-temurin:21-jre-jammy
ARG JAR_FILE=authee-core/build/libs/authee-core.jar
COPY ${JAR_FILE} /app/authee.jar
ENTRYPOINT ["java","-XX:+UseContainerSupport","-Xms256m","-Xmx1024m","-jar","/app/authee.jar"]
```

(We recommend using `jib` or distroless images for smaller footprint.)

## 13. CI/CD & Release

* CI: GitHub Actions with matrix (jdk 21), build, unit/integration tests (Testcontainers), OWASP scan, build image.
* CD: Image pushed to registry; manifests updated via Helm charts; deploy via ArgoCD/Flux (GitOps) for production.
* Canary & Blue/Green deployments supported via Kubernetes and Istio/ALB.

## 14. Security & Compliance Practices

* Threat model per milestone; run SAST/DAST and pen-tests prior to GA.
* Secrets management with Vault/KMS; no secrets in repo.
* Audit export and retention policies configurable per tenant.

## 15. Developer DX & Utilities

* `authee-cli` (Java/Node) to seed tenants/clients for dev.
* Local mock IdP server (a small spring-boot test service) to simulate SAML/OIDC flows.
* Flow debugger endpoint with access-controlled logs/traces.

## 16. Example Class Sketches

* `AuthOrchestrator` — orchestrates flow
* `OAuth2Handler` implements `ProtocolHandler`
* `PolicyEvaluator` — evaluates policies and returns `PolicyDecision`
* `RiskService` — aggregates signals and returns `RiskScore`
* `TokenService` — `issueToken()`, `introspectToken()`

## 17. Mermaid Diagrams (for README)

Include the diagrams from central HLD (OAuth flow + Logical Diagram). They are valid Markdown Mermaid and will render on GitHub.

## 18. Next Immediate Tasks (Sprint 0)

1. Create Gradle multi-module skeleton and CI pipelines.
2. Implement `authee-shared`, `authee-core` (bootstrap app), `authee-protocols` with OAuth2 endpoints (authorize/token) skeleton.
3. Wire Postgres + Redis containers via Testcontainers and create basic integration test.
4. Implement per-tenant key model and JWKS endpoint stub.
5. Add OpenTelemetry auto-instrumentation and basic dashboards.

---

### Appendix — Helpful Libraries & Links

* Spring Boot: [https://spring.io/projects/spring-boot](https://spring.io/projects/spring-boot)
* Spring Security SAML: [https://spring.io/projects/spring-security-saml](https://spring.io/projects/spring-security-saml)
* Nimbus JOSE JWT: [https://connect2id.com/products/nimbus-jose-jwt](https://connect2id.com/products/nimbus-jose-jwt)
* OpenTelemetry Java: [https://opentelemetry.io/docs/java/](https://opentelemetry.io/docs/java/)
* Testcontainers: [https://www.testcontainers.org/](https://www.testcontainers.org/)

---

*Prepared by: Principal Engineer — Authee Implementation (Java)*
