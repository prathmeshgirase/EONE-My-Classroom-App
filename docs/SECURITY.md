# Security notes

## Implemented

- BCrypt password hashing.
- Short-lived JWT access tokens.
- Opaque refresh tokens stored only as SHA-256 hashes in PostgreSQL.
- Refresh-token rotation.
- Stateless Spring Security chain.
- Department/classroom/class-subject authorization checks.
- Ownership checks for student submissions and notifications.
- DTO validation.
- Audit logging for key administrative/academic mutations.
- CORS configured by environment variable.

## Before public production

- Replace the development JWT secret with a long random secret from a secret manager.
- Set `EONE_SEED_ENABLED=false`.
- Use HTTPS end to end.
- Add rate limiting at the gateway/reverse proxy, especially for `/auth/login` and `/auth/refresh`.
- Configure database backups and least-privilege credentials.
- Add malware/content controls if binary uploads are enabled.
- Add real push credentials only through secrets; never commit Firebase/APNs credentials.
- Review institutional privacy/retention requirements before storing student academic records.
