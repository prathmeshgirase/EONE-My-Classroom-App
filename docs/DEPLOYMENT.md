# Local and production deployment

## Local Docker environment

Copy `.env.example` to `.env` only if you do not already have a configured `.env`. The template contains environment assignments only.

```powershell
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

The Compose API is `http://localhost:8081/api/v1`, Swagger is `http://localhost:8081/swagger-ui.html`, and health is `http://localhost:8081/actuator/health`.

Normal shutdown preserves PostgreSQL data:

```powershell
docker compose down
```

Do not add volume deletion to routine shutdown. Database reset is a separate destructive maintenance decision requiring a verified backup.

Android emulator:

```powershell
cd mobile
flutter run --dart-define=EONE_API_URL=http://10.0.2.2:8081/api/v1
```

USB-connected Android device:

```powershell
adb reverse tcp:18080 tcp:8081
cd mobile
flutter run --dart-define=EONE_API_URL=http://127.0.0.1:18080/api/v1
```

For LAN-connected devices, use the development computer's LAN address and port 8081. Debug Android allows HTTP; production must use HTTPS. Standalone Maven startup uses backend port 8080 unless PORT is configured, so set the mobile URL accordingly.

Demo seeding is explicitly enabled by local Compose. Standalone backend startup defaults to no seeding. The login UI shows demo credentials only with `--dart-define=EONE_SHOW_DEMO_ACCOUNTS=true`.

## Production requirements

Set `SPRING_PROFILES_ACTIVE=production`, DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, EONE_JWT_SECRET and EONE_CORS_ORIGINS through the hosting environment. Use an unpredictable JWT secret of at least 64 bytes and explicit HTTPS CORS origins. The production guard rejects demo seeding, known placeholder secrets, short secrets and insecure/wildcard origins. API documentation is disabled by default in production. Health probes remain public without internal details.

A trusted reverse proxy or managed host must terminate HTTPS; the production profile does not install a certificate. Use managed database backups and least-privilege credentials. Do not reuse known demo credentials for public access. Preserve existing data when moving environments; no database reset is required by these changes.

Before V2, back up the database and inspect duplicate active scoped roles, invalid scope shapes, nonpositive assignment marks, invalid term dates and chapter positions. V2 deliberately fails if invalid legacy data exists. Resolve ambiguities with the institution; never silently delete or reassign rows to force migration success.

Release mobile builds require an explicit HTTPS `EONE_API_URL`. Android release signing still uses the existing development configuration and must be replaced with institution-owned release credentials before distribution. FCM and email recovery remain implementation work, not working integrations awaiting only credentials.

File uploads use `EONE_STORAGE_DIRECTORY` (default `./data/uploads`). Compose persists files in the `eone_uploads` volume. Back up this storage together with PostgreSQL. A public deployment must provide persistent storage, quotas, scanning and retention policies; multi-instance hosting needs shared storage or an object-storage adapter. See [upload report](UPLOAD_REPORT_2026-10-07.md).

No public deployment has been performed. Docker startup has not been verified on this machine because Docker is unavailable.
