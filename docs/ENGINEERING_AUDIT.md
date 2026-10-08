# EONE engineering audit — 6 October 2026

This is the pre-change baseline and implementation backlog, not a production-readiness certification.

## Baseline

- The supplied folder is not a Git checkout (`git status` and branch lookup fail). Existing files and database data must be preserved; no history was initialized or reset.
- Java 21 and Maven are installed. Three backend tests pass (two mocked access tests and one H2 context test). Initial packaging was blocked by dependency-cache permissions; rerun uses the existing user cache with authorized access.
- Flutter analysis passes without suppressions. Its single test is a Flutter harness smoke test, not an EONE workflow test.
- Docker is absent from PATH and the standard Docker Desktop installation path. Container startup and PostgreSQL migration execution are unverified.
- Existing validation documentation describes an older environment and is not evidence of current runtime verification.

## Architecture and implemented functionality

Flutter uses Riverpod, Dio, secure token storage, typed response models, feature screens, and an in-memory classroom selector. Spring Boot 3.5 uses Java 21, stateless JWT security, BCrypt, hashed rotating refresh tokens, JPA repositories, DTOs and Flyway. PostgreSQL contains 19 tables. Docker Compose provides a persistent database volume. Two GitHub workflows run backend and mobile checks.

Implemented flows include pending registration, approval/suspension APIs, academic structure creation, enrollment, scoped staff roles, chapters, URL materials, assignment creation/submission/grading, attendance recording, classroom announcements and an in-app inbox. HOD management screens cover creation and approval. This is code inspection evidence; full end-to-end operation is not yet established.

Preserve the stack, scoped HOD/class/subject distinctions, one identity with several responsibilities, hashed refresh tokens, LAZY relationships, DTO contracts, Flyway V1, disabled Open Session in View, existing physical-device configuration and persistent database volume.

## Findings

### Critical

- Production startup currently permits default JWT secrets and demo account seeding. Public deployment must fail closed or use explicit production settings.

### High

- `MeController.dashboard` collects every subject's assignments from a teacher's visible classroom, bypassing subject visibility.
- Direct assignment reads/submissions do not restrict unpublished drafts. Resubmitting graded work overwrites answer/status while retaining marks and grader.
- Refresh consumption has no lock or atomic compare-and-update. Concurrent refresh requests can both succeed; mobile independently refreshes concurrent failures.
- Spring Security does not explicitly return 401 for unauthenticated API calls, which the mobile refresh interceptor depends on.
- Role uniqueness is only checked in Java and can race. Schema lacks role-scope shape checks.
- Attendance nested marks lack cascading validation; duplicate marks fail at persistence rather than validation. Duplicate session protection is missing.
- Tests do not cover endpoint authorization, cross-department access, token concurrency or PostgreSQL migrations. H2 disables Flyway.
- Authentication state/cache isolation and transient network recovery need hardening. Restore clears credentials on all errors.
- File upload/storage, FCM, password change/recovery, session management, and release signing are incomplete or absent.

### Medium

- Business mutations and transactions are concentrated in controllers. Repeated DTO mapping and compressed source reduce maintainability.
- Most list endpoints are unbounded. Dashboard and attendance load data and aggregate/filter in Java; chapter/material and assignment/submission queries create N+1 patterns.
- `read-all` only processes the newest 100 notifications.
- Subject lists expose inaccessible subjects to subject-only teachers, leading to dead-end navigation.
- Grading UI does not show the submitted answer/attachment before grading; invalid numbers silently become zero.
- Drafts cannot be listed/published/closed through a complete lifecycle; submission history and metrics are missing.
- HOD/class-teacher attendance overviews, audit viewer, search/filter, removal flows and dashboard statistics are absent.
- Department/subject announcements can be created but have no complete retrieval/navigation experience.
- Staff accounts all see Manage even without an HOD role. Classroom selection is not persisted; role selection is absent.
- Theme is light-only; no adaptive rail or shared state components. Large fixed dialogs/grids require small-screen/text-scale testing.
- HTTP/data integrity errors fall through to generic 500; unexpected errors are not logged for diagnosis.
- CI suppresses analyzer warnings, omits format and Android builds, and backend CI does not package.
- README advertises port 8080 while Compose publishes 8081. Docker backend lacks health check/non-root runtime. Production configuration and actual deployment remain unverified.

### Low

- Demo material points to example.com. Native runner instructions are stale now that runners exist. Interview and screenshot guidance is missing.

## Target architecture

Retain Flutter feature modules with session-scoped providers, typed API clients, a shared accessible design system, capability-driven adaptive navigation and persistent validated classroom context. Retain Spring controllers as transport adapters; move academic mutations to transactional services with explicit access policy. Use PostgreSQL constraints and query projections, bounded DTO pages, immutable submission history where policy requires it, and additive Flyway migrations. Add a storage abstraction with an authenticated local provider and production object storage, and durable notification delivery separated from academic commits. Production remains environment-driven and HTTPS-only.

## Ordered implementation plan

1. **Authorization and integrity:** access/dashboard/classroom/assignment controllers, refresh repository, DTO validation and security entry point. Preserve response shapes; add negative endpoint and concurrency tests. No schema changes for the initial fixes. Risk: permission regressions; test multi-role combinations.
2. **Database and service boundaries:** additive V2 constraints/indexes after checking existing data; PostgreSQL clean/upgrade tests; transactional academic services, query projections, consistent errors and pagination with matching mobile consumers. Risk: invalid legacy rows must stop migration with diagnostics, never be silently deleted.
3. **Academic workflows:** lifecycle, submission policy/history, grading evidence/metrics, attendance analytics, scoped announcements, HOD audit/search/removal. Backend DTOs and Flutter screens evolve together; lifecycle/history need migrations and integration tests.
4. **Account lifecycle:** single-flight refresh, transient offline recovery, session isolation, password change/reset and throttling. Token/session schema changes and security tests required; mail credentials are external.
5. **Uploads and push:** metadata migration, authorized upload/download, local/S3 adapters, device registrations and durable FCM delivery. Update material/submission UI and tests. External credentials required only for live-provider validation.
6. **Design system then screens:** shared tokens/components, light/dark, adaptive navigation, role and classroom contexts, accessible forms and all states. Widget/provider tests across viewport/text scales; no schema change.
7. **Hardening and release:** strict CI, PostgreSQL migration matrix, packaged backend/Android build, Docker health, explicit production environment, load/accessibility/manual QA. No deployment claims without a live result.
8. **Documentation and final review:** update API/RBAC/schema/setup/validation/traceability, record remaining risks, truthful resume/interview material and screenshots from actual builds.

Each increment must pass relevant tests before the next dependent change. A check is not complete merely because code exists. External provider credentials, Android signing and absent Docker runtime remain explicit validation gates.
