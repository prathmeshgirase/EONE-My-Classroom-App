# Testing

The authoritative commands and execution evidence are in [VALIDATION.md](VALIDATION.md).

`mvn verify` runs 29 backend tests and builds the application. Standard tests use H2 for quick application checks. `mvn -Ppostgres verify` additionally runs two PostgreSQL-specific Flyway integration tests using EONE_TEST_DATABASE_URL, EONE_TEST_DATABASE_USERNAME and EONE_TEST_DATABASE_PASSWORD. Supply a disposable database: these tests create isolated schemas and intentionally retain them for diagnosis. They do not touch the normal application database unless misconfigured.

Coverage includes cross-subject dashboard visibility, unpublished assignments, graded-submission immutability, unenrolled access, HOD department boundaries, independent classroom/subject responsibilities, attendance validation, notification ownership, malformed identifiers, production guard settings and refresh concurrency. PostgreSQL tests cover clean migration, existing-data upgrade, partial role uniqueness and scope constraints.

Flutter's 13 tests cover session refresh concurrency/error handling and login form validation/accessibility. They replace the original placeholder harness test. Run flutter analyze, flutter test, and dart format --output=none --set-exit-if-changed lib test. Build Android with flutter build apk --debug.

Missing coverage remains substantial: full role workflows, native upload dialogs and push integrations, permission changes during active sessions, full mobile state/navigation, broader viewports, performance, and production deployment. See the manual checklist in [IMPLEMENTATION_REPORT.md](IMPLEMENTATION_REPORT.md).
