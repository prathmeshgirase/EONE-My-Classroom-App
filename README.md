# EONE — Department-Aware Classroom Management Platform

Current status: [implementation and verification](docs/COMPLETION_WORK_IN_PROGRESS.md), [cloud setup](docs/EONE_CLOUD_SETUP.md). The full production-readiness brief remains incomplete; configured cloud resources are not yet a live deployment.

EONE is a full-stack college classroom management system designed around real academic responsibilities instead of a single flat "teacher" role.

## What is included

- **Flutter mobile app** for students, subject teachers, class teachers and HODs.
- **Spring Boot REST API** with JWT access tokens, rotating refresh tokens and scoped authorization.
- **PostgreSQL database** managed through Flyway migrations.
- **Docker Compose** for PostgreSQL + backend.
- **Seeded demo data** showing a multi-role HOD, a teacher assigned across multiple year groups, a student and a pending user.
- **OpenAPI/Swagger**, Actuator health endpoint, audit log, validation and centralized API errors.
- **GitHub Actions** for backend tests and Flutter analysis/tests.
- Product, architecture, database, API, security, testing, deployment and validation documentation.

## Real EONE role model

A user has one account and may carry several scoped responsibilities simultaneously:

- `HOD` — department administration and oversight.
- `CLASS_TEACHER` — class-level responsibility for a specific classroom.
- `SUBJECT_TEACHER` — content, assignment, grading and attendance responsibility for one classroom subject.
- `STUDENT` — classroom membership through enrollment.

These roles are deliberately independent. An HOD who teaches DSA is assigned both `HOD` and `SUBJECT_TEACHER`; HOD authority alone does **not** grant the right to grade every subject. A class teacher may oversee the classroom but does not automatically become every subject's teacher.

## Main workflows

### HOD

1. Approve/suspend department users.
2. Create programs, terms, classrooms and subjects.
3. Link subjects to classrooms.
4. Enroll students.
5. Assign HOD, class-teacher and subject-teacher responsibilities.
6. View department classrooms and publish class-level announcements.

### Subject teacher

1. Open only assigned classrooms/subjects.
2. Create chapters and learning material links.
3. Publish assignments.
4. View submissions and grade them with feedback.
5. Take subject attendance.
6. Students receive in-app notifications for new content, assignments, grades and absences.

### Class teacher

1. View the full class roster and all subjects for the assigned classroom.
2. Monitor the class without receiving automatic subject-teacher edit permissions.
3. Publish classroom announcements.
4. Switch between classrooms when assigned to more than one.

### Student

1. View the enrolled classroom and subjects.
2. Open chapters/materials.
3. View and submit assignments.
4. See grades and teacher feedback.
5. View own subject attendance records.
6. Receive and mark notifications as read.

## Repository layout

```text
eone/
├── backend/                 Spring Boot API
│   ├── src/main/java/       domain, security, services, controllers
│   └── src/main/resources/  application config + Flyway migration
├── mobile/                  Flutter application source
│   ├── lib/                 complete app code
│   ├── test/                widget smoke test
│   └── scripts/             native Flutter bootstrap helpers
├── docs/                    architecture/product/security/testing docs
├── docker-compose.yml       PostgreSQL + backend
└── .github/workflows/       CI
```

## Run locally

### Prerequisites

- Docker Desktop (recommended for backend/database)
- Flutter SDK for the mobile app
- Android Studio/Android SDK for Android development

### 1. Configure environment

```powershell
Copy-Item .env.example .env
```

Change `EONE_JWT_SECRET` before using the project outside local development.

### 2. Start PostgreSQL + Spring Boot

```powershell
docker compose up --build
```

Backend: `http://localhost:8081`

Swagger UI: `http://localhost:8081/swagger-ui.html`

Health: `http://localhost:8081/actuator/health`

### 3. Generate the native Flutter runner files once

The generated Android/iOS runner boilerplate depends on the Flutter SDK installed on the developer machine. The source package includes a safe helper that generates those files while preserving the EONE code:

```powershell
cd mobile
.\scripts\bootstrap_native.ps1
```

### 4. Run the mobile app

Android emulator:

```powershell
flutter run --dart-define=EONE_API_URL=http://10.0.2.2:8081/api/v1
```

Physical Android device: replace `10.0.2.2` with the development machine's LAN IP.

## Demo accounts

All seeded demo passwords are `Eone@123`.

| Account | Purpose |
|---|---|
| `hod@eone.dev` | HOD + SY class teacher + SY DSA subject teacher |
| `teacher@eone.dev` | Subject teacher across FY, SY and TY |
| `student@eone.dev` | Active SY student |
| `pending@eone.dev` | Pending student for HOD approval demo |

Seed data is enabled by Docker Compose for local development and only runs when the database is empty. Set `EONE_SEED_ENABLED=false` for a clean or production database.

## Production-sensitive items

Do not commit secrets. Use HTTPS, a strong external JWT secret, managed PostgreSQL backups, an object-storage provider for real uploaded files, and a push-notification provider such as FCM/APNs for OS-level notifications. EONE already implements persistent in-app notifications; the notification service contains the explicit integration point for a production push adapter.

See `docs/DEPLOYMENT.md` and `docs/SECURITY.md` before publishing a public demo. The exact pre-package verification status and toolchain limitations are recorded in `docs/VALIDATION.md`.


## Engineering status

The initial audit and first hardening increment are documented in [the implementation report](docs/IMPLEMENTATION_REPORT.md). EONE is not yet production-ready; remaining product workflows and verification gaps are listed explicitly. See [validation evidence](docs/VALIDATION.md) and [the ordered plan](docs/ENGINEERING_AUDIT.md).
