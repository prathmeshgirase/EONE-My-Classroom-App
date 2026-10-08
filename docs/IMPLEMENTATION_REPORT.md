Latest continuation: [File upload report — 7 October 2026](UPLOAD_REPORT_2026-10-07.md). The [assignment and attendance report](FEATURE_REPORT_2026-10-07.md) and report below record earlier checkpoints.

# Implementation report — verified hardening checkpoint

This checkpoint implements the audit and an initial correctness/security increment. It does not complete the full product brief. See `ENGINEERING_AUDIT.md` for the initial architecture review, severity-ranked issues, target architecture and dependency-aware plan; `EONE_REQUIREMENTS.md` preserves the complete supplied brief.

## Changes delivered

- Dashboard assignments and classroom subject lists enforce subject visibility. HOD and class-teacher oversight remain separate from teaching permission.
- Draft assignments require teaching access; students cannot submit drafts. Graded submissions cannot be overwritten by students. Nonfinite grade values are rejected.
- Refresh-token consumption uses a pessimistic database lock. A concurrent integration test proves only one consumer succeeds. Mobile shares one rotation across concurrent failures and preserves tokens on network failures.
- Logout during an in-flight refresh is covered. Login/logout invalidate private dashboard/classroom/inbox context. Session restoration offers retry when connectivity fails instead of silently clearing credentials.
- Anonymous API calls return 401. Invalid JSON/identifiers return 400; data conflicts return 409. Unexpected failures log exception type without bodies or tokens.
- Attendance marks validate nested fields and reject duplicate students. Dates cannot be in the future.
- Mark-all-read updates every unread notification for the current user, including older than the first 100, using one database update.
- V2 adds exact role-scope shape checks, partial unique indexes for active responsibilities, sensible marks/date/position checks, and indexes matching materials, attendance and classroom-announcement lookups. V1 is unchanged. No legacy rows are automatically deleted or repaired.
- Production profile requires explicit database/JWT/CORS configuration, disables demo seeding and Swagger, and provides health probes. A guard rejects unsafe known defaults. Standalone development no longer seeds unless explicitly requested; Compose retains its local demo behavior.
- Android main manifest includes Internet permission. Release API configuration rejects non-HTTPS URLs. Debug emulator default and README agree with Compose's published port 8081.
- Login has validation, password visibility labels, autofill, no prefilled demo password, and a tested small-phone/large-text layout. Demo-account text is opt-in.
- Grading displays submitted text/attachment and validates marks instead of silently assigning zero. Staff without teaching rights do not see student submission controls. Manage is shown only for an HOD responsibility.
- Dart source is formatted. CI now fails on analyzer findings, checks formatting, packages the backend, runs PostgreSQL clean/upgrade tests and requests an Android debug build.
- `.env.example` contains only configuration assignments. Local/deployment/test instructions and current evidence are documented.

## Verification

- Backend package: PASS (`mvn -Ppostgres verify`, separate `target/verification` output).
- Standard backend tests: **18 passed**, zero failures/errors/skips.
- PostgreSQL migration integration tests: **2 passed**, zero failures/errors/skips.
- PostgreSQL 18 application checkpoint: **16 tests passed** using real Flyway migrations and Hibernate validation; see `VALIDATION.md` for precise scope.
- Flutter tests: **6 passed**.
- Flutter analyze: **no issues**.
- Dart format check: **23 files, zero changes required**.
- Android debug build: **PASS**. Artifact: `mobile/build/app/outputs/flutter-apk/app-debug.apk`.
- Android tooling reports an SDK XML version warning; this did not prevent the debug build. Align Android Studio and command-line tools before release qualification.
- Docker startup, GitHub-hosted CI, signed Android release, iOS and live deployment: **not verified**.

The backend artifact is `backend/target/verification/eone-backend-1.0.0.jar`. Build output is ignored; source and tests are the deliverable. No Git metadata exists in this folder, so no commits or PRs were created.

## Remaining implementation

Do not describe the following as complete or merely waiting for credentials:

- Complete lifecycle including publish/edit/close, resubmission policy/history and submission metrics.
- Real uploads, attachment ownership/download rules, local/object-storage adapters and mobile file picking.
- FCM device registrations, durable delivery/retries and deadline scheduling.
- Password change/reset, email recovery, throttling and full session management.
- Pagination and server-side filtering across growing collections.
- Classroom/HOD attendance analytics and efficient dashboard projections; several existing list paths still use N+1 queries.
- HOD audit UI, administration removal/edit flows and complete announcement audiences.
- Persistent classroom and role context, coherent design tokens, dark theme, adaptive navigation and broad accessibility/offline states.
- Moving academic business transactions out of controllers into services.
- End-to-end physical-device role QA, performance measurements, screenshots and complete recruiter documentation.

V2 deliberately refuses invalid legacy role rows. Inspect and resolve those with an administrator before upgrading a populated database. Duplicate attendance sessions are not yet prevented. Concurrent assignment mutation policies require further work. The UI and backend still need a full acceptance pass.

## Manual QA checklist

1. Use the existing locally seeded accounts (`student@eone.dev`, `teacher@eone.dev`, `hod@eone.dev`; development password `Eone@123`) only in a private demo database.
2. Teacher: confirm the SY DSA assignment is absent when only SY DBMS is assigned; FY/TY assigned subjects remain visible.
3. HOD: confirm management is visible and DSA teaching actions appear only through the explicit DSA role.
4. Student: submit published work; teacher reviews answer and grades; student sees feedback. Confirm graded work cannot be replaced.
5. Expire an access token and trigger concurrent requests; confirm one refresh and successful retries. Disconnect the network during startup and use Retry.
6. Sign out and sign in as a different user; confirm no cached classroom/inbox is displayed from the prior account.
7. Check login on a narrow phone with large system text, keyboard open and password visibility enabled.
8. Test a populated database upgrade on a backup copy before applying V2 to valuable data.
9. Complete the remaining acceptance criteria in `EONE_REQUIREMENTS.md` before public deployment.

## Honest interview material for this checkpoint

EONE uses one identity with independent department, classroom and class-subject responsibilities. The hardening work demonstrates backend object-level authorization, race-safe refresh rotation, database-enforced scoped-role integrity and regression tests across Java, Flutter and PostgreSQL.

Possible resume bullets after independently reviewing the work:

- Hardened a Spring Boot/Flutter academic platform with scoped authorization and endpoint regression coverage for student, teacher and department boundaries.
- Added PostgreSQL role constraints and tested clean and incremental Flyway migrations while preserving existing records.
- Implemented coordinated mobile token refresh and tested offline recovery, invalid sessions and accessible login behavior.

Do not claim production deployment, push notifications, object storage, measured performance gains or complete end-to-end product readiness. Capture real screenshots only after the corresponding workflows are finished: role dashboards, context switching, grading, attendance, HOD approvals, dark theme and mobile/tablet layouts.

## External setup still needed later

Institution-owned hosting/database credentials, object-storage account, Firebase project and service identity, email delivery credentials, HTTPS domain/certificate and Android release signing. These do not substitute for finishing the missing integrations. Local commands are in `DEPLOYMENT.md`; test commands are in `VALIDATION.md`.
