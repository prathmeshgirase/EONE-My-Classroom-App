# File upload increment — 7 October 2026

## Implemented

Teachers can upload PDF, PNG and JPEG learning materials into a chapter. Students can attach one uploaded file when submitting or revising an assignment. The app shows upload progress, validation errors and download actions on materials, current submissions, grading and previous versions. Existing external attachment links remain supported.

The server limits files to 10 MiB, verifies an allowed extension and matching magic bytes, sanitizes display filenames, generates independent storage keys and serves downloads as attachments with no-store and nosniff headers. Signature checks are not malware scanning or complete document validation.

Downloads require authentication and current subject access. Material files follow subject visibility. Submission files are restricted to their owner and the assigned subject teacher; HOD oversight alone cannot download student work. Internal attachment reuse checks owner and assignment. Previous submission files remain available through history.

Upload metadata and the academic mutation share a database transaction. A rolled-back transaction removes its new blob; an application crash between filesystem write and transaction completion can still leave an orphan. Reconciliation, institutional quotas, malware scanning, retention/deletion policy and an S3/R2 adapter remain future work. `FileStorage` currently has a local-disk implementation only.

V4 adds `stored_files` without changing V1–V3 or existing submissions. No login or session implementation was changed during this increment.

## Storage configuration

Standalone backend: `EONE_STORAGE_DIRECTORY` defaults to `./data/uploads`, relative to the backend process working directory. Choose a persistent writable directory; never serve it directly from a web server. Compose mounts the `eone_uploads` named volume at `/app/data/uploads`. Back up database metadata and file storage together. Do not use `docker compose down -v` for routine shutdown because it deletes persistent volumes.

Local storage is suitable for a single backend instance with persistent disk. Multiple instances need shared durable storage or a new object-storage adapter. No cloud storage provider is configured.

## Verification

- 29 backend tests pass on H2 and on the isolated PostgreSQL 18 validation database.
- Two PostgreSQL migration tests pass through V4, including upgrade from V1 with existing user/role data.
- Tests cover scoped download access, revoked enrollment, submission history attachments, attachment ownership, size/type rejection, transaction rollback cleanup and storage path/collision handling.
- Flutter analysis is clean and all 13 tests pass. New tests check multipart body generation, API-local downloads, returned filename/bytes and link rendering.
- Android debug APK builds successfully at `mobile/build/app/outputs/flutter-apk/app-debug.apk`. The plugin Kotlin compatibility warning remains; the scoped Gradle fix is explained below. Native file-picker/save dialogs still require a real-device check. Docker and iOS were not executed.
- The isolated PostgreSQL validation server was stopped after verification and the test upload directory contains no remaining files. The existing application database and login/session implementation were not changed.

## Device acceptance steps

1. Restart the updated backend so Flyway applies V4; use the updated mobile build.
2. As an assigned subject teacher, open Subject → Create → Upload material file. Choose a chapter, title and PDF/image, then publish.
3. As an enrolled student, open that material and choose Download attachment; save it and open it from the device's files app.
4. Submit an open assignment with a chosen file. Before its deadline, edit the submission and attach another file. Download the old attachment from Previous versions.
5. As the assigned teacher, open grading and download the student's attachment. Grade or close the assignment and verify further student revisions are rejected.
6. Try a file over 10 MiB and a renamed non-PDF file; verify an actionable error and no published material/submission.

## Remaining project work

FCM delivery/device registration, password recovery, broad search/pagination, remaining analytics and administration screens, persistent role/classroom selection, dark/adaptive UI, wider device/accessibility tests, production signing/deployment and the storage production controls above remain unfinished. This report records a verified feature increment, not completion of the full original brief.

## Android file-picker compatibility

The installed file_picker 11.0.3 skips applying Kotlin on AGP 9. This app retains `android.builtInKotlin=false`, so the root Android Gradle script explicitly applies KGP to that plugin and targets Java 17. The compatibility block is scoped to file_picker and only runs while built-in Kotlin is disabled. Revisit it when upgrading Flutter/plugin versions; see the [Flutter migration guide](https://docs.flutter.dev/release/breaking-changes/migrate-to-built-in-kotlin/for-app-developers). No package-cache source was edited.
