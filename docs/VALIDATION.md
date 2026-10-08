# Validation report — 6 October 2026

## Scope

This report covers the initial audit and first hardening increment. EONE is **not yet production-ready** and the full requirements in `EONE_REQUIREMENTS.md` are not complete. The ordered backlog is in `ENGINEERING_AUDIT.md`.

## Evidence

- Baseline: three backend tests and one placeholder Flutter test passed; Flutter analysis was clean.
- Backend: 18 tests now cover endpoint authorization, drafts, immutable graded submissions, malformed attendance, notification ownership, production guards and concurrent refresh consumption, alongside the original tests.
- PostgreSQL: the 16-test suite at the database-hardening checkpoint passed against PostgreSQL 18 with Flyway enabled and Hibernate validation. The later two role tests also pass in the standard suite.
- Two additional PostgreSQL integration tests verify clean migration, V1-to-V2 upgrade with preservation of an existing user, active-role uniqueness, valid scope shape and historical inactive assignments. Run with `-Ppostgres`; these checks require an explicitly supplied disposable database and are not silently skipped when requested.
- Flutter: six tests cover concurrent refresh, invalid refresh, transient connectivity, logout during refresh, login validation and a 320px phone with 2x text scale.
- Current final analyzer/format/APK results are recorded in `IMPLEMENTATION_REPORT.md` after execution.
- No tests were disabled to obtain a green result.

## Environment and safety

The project folder has no `.git` metadata. No Git history was initialized, overwritten or pushed. `.env` was not changed. The original Flyway V1 was preserved. No existing user database or Docker volume was altered.

Docker is not installed at its standard path or available on PATH, so Docker startup is unverified. PostgreSQL 18 is installed. Validation used a separate cluster under `backend/target/postgres-validation`, listening only on 127.0.0.1:55439; it does not use the existing PostgreSQL service or application database. Never run Maven clean while that temporary server is running. The verification server is stopped after checks.

The shared `target/classes` directory once contained an invalid Java class file. Building into `target/verification` produced valid compiled classes and successful tests. The writer of the invalid class file was not established; an IDE build-output collision is a possible cause.

## Repeatable commands

Backend without PostgreSQL integration tests:

```powershell
cd backend
mvn --batch-mode verify
```

On this Windows machine, Maven defaults to an inaccessible `C:\.m2` location. Use the existing user cache and a separate output directory if needed:

```powershell
mvn '-Dmaven.repo.local=C:\Users\prath\.m2\repository' '-Deone.build.directory=target/verification' --batch-mode verify
```

PostgreSQL clean/upgrade tests, against a disposable database only:

```powershell
$env:EONE_TEST_DATABASE_URL='jdbc:postgresql://localhost:5432/eone_test'
$env:EONE_TEST_DATABASE_USERNAME='eone_test'
$env:EONE_TEST_DATABASE_PASSWORD='YOUR_TEST_PASSWORD'
mvn --batch-mode -Ppostgres verify
```

Flutter:

```powershell
cd mobile
flutter pub get
dart format --output=none --set-exit-if-changed lib test
flutter analyze
flutter test
flutter build apk --debug
```

## Unverified acceptance criteria

Full role workflows on a physical device, complete mobile accessibility, performance/load, Docker startup, CI execution on GitHub, PostgreSQL 17 execution, Android release signing, iOS builds, external object storage, Firebase delivery and public HTTPS deployment remain unverified. Several corresponding features are still unimplemented; missing code is not an external-credential blocker.

## 7 October continuation

The latest increment passed 23 backend tests against PostgreSQL 18, two clean/upgrade migration tests through V3, and 10 Flutter tests. Analysis is clean. See [the continuation report](FEATURE_REPORT_2026-10-07.md) for the exact changes and limitations. Login code was left unchanged at the user's request.

## Upload checkpoint — 7 October 2026

29 backend tests passed on H2 and PostgreSQL 18; two Flyway tests passed through V4. Evidence: backend/upload-verification.log and backend/upload-postgres-verification.log. Flutter analysis reported no issues; 13 tests passed. Android debug APK build succeeded after the scoped file-picker Kotlin compatibility fix. See UPLOAD_REPORT_2026-10-07.md for scope and device acceptance steps. These supersede earlier test counts; native dialogs and public deployment remain unverified.
