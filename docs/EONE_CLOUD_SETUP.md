# EONE cloud setup

Verified 7 October 2026. Deployment is not yet complete.

## Supabase

- Project: EONE (`jraeagiqpwswhebxvtzg`), healthy, Tokyo.
- Project URL: `https://jraeagiqpwswhebxvtzg.supabase.co`
- Bucket: `eone-private`, public access disabled, 10 MB per-file limit, no client access policies.
- Backend storage provider: `supabase`.
- IPv4-compatible JDBC URL: `jdbc:postgresql://aws-0-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require`
- Database username: `postgres.jraeagiqpwswhebxvtzg`
- Password: user-held; enter directly into the deployment secret field. Never commit it.

Use the session pooler for the persistent Spring Boot connection pool. Do not use the transaction pooler for schema migrations. The application runs Flyway on startup; no application migrations have run against this hosted database yet.

The backend requires a server-only Supabase service key. Do not put this key into Flutter or source control. Storage remains inaccessible through EONE until the backend is configured and deployed.

## Firebase

Project `eone-3dbf5`; Android package `com.example.eone_mobile`, nickname EONE Android. Registration is complete. Client configuration and backend credentials remain to be connected; live push is unverified.

## Remaining deployment inputs

- Source repository for Render.
- Database password entered privately into Render.
- Server-only storage key, generated JWT secret, permitted CORS origins.
- Verified Brevo sender and server API key for recovery mail.
- Firebase backend credentials and mobile build configuration.
- Hosted migrations, health check and end-to-end acceptance tests.

Keep credentials out of chat, screenshots, logs, and tracked files. Current cloud provisioning does not imply that the original feature brief is complete.
