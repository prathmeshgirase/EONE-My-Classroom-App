# API summary

Base path: `/api/v1`

## Authentication

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

## Current user

- `GET /me`
- `GET /me/contexts`
- `GET /me/dashboard`

## Classrooms

- `GET /classrooms`
- `GET /classrooms/{id}`
- `GET /classrooms/{id}/subjects`
- `GET /classrooms/{id}/students`
- `GET /classrooms/{id}/announcements`

## Subject workspace

- `GET /class-subjects/{id}`
- `GET /class-subjects/{id}/chapters`
- `POST /class-subjects/{id}/chapters`
- `POST /class-subjects/chapters/{chapterId}/materials`
- `GET /class-subjects/{id}/assignments`
- `POST /class-subjects/{id}/assignments`

## Assignments

- `GET /assignments/{id}`
- `POST /assignments/{id}/submit`
- `GET /assignments/{id}/submissions`
- `PATCH /assignments/submissions/{submissionId}/grade`

## Attendance

- `POST /attendance/class-subjects/{id}/sessions`
- `GET /attendance/class-subjects/{id}/sessions`
- `GET /attendance/me`

## Announcements and notifications

- `POST /announcements`
- `GET /notifications`
- `POST /notifications/{id}/read`
- `POST /notifications/read-all`

## HOD department administration

All HOD endpoints are prefixed `/hod/departments/{departmentId}`.

- list/approve/suspend users
- list/create programs
- list/create terms
- list/create classrooms
- list/create subjects
- link class-subjects
- enroll students
- assign/deactivate scoped staff roles

The complete interactive contract is generated at `/swagger-ui.html` while the backend is running.

## Assignment lifecycle additions (7 October 2026)

- `PATCH /assignments/{id}`: assigned subject teacher edits a draft. Body: title, description, dueAt, maxMarks.
- `POST /assignments/{id}/publish`: assigned subject teacher publishes a draft; repeated calls do not repeat notifications.
- `POST /assignments/{id}/close`: assigned subject teacher stops submissions; existing grading remains available.
- `GET /assignments/{id}/statistics`: assigned subject teacher receives students, submitted, notSubmitted, graded, pendingGrading and late counts for active enrollment.
- `GET /assignments/submissions/{submissionId}/history?page=0&size=20`: submission owner with current subject access or the assigned teacher receives earlier versions.

Assignment responses retain their prior fields and add nullable closedAt and server-derived state (DRAFT, OPEN, OVERDUE, CLOSED). Subject assignment lists include drafts only for assigned subject teachers. Edits are restricted to drafts; resubmission is restricted to ungraded work before the deadline. First late submissions remain allowed until closed.

History uses `{content, page, size, totalElements, totalPages}`; size is 1–100. Each history item contains id, answerText, attachmentUrl, submittedAt and status. Invalid paging returns 400; access violations return 403; invalid lifecycle transitions return 409.

## Classroom attendance overview

`GET /attendance/classrooms/{classroomId}/summary?page=0&size=20&below=101` requires classroom-management access (class teacher or department HOD). Use below=75 for records below 75%; 101 means all active students. Rows contain studentId, studentName, present, absent, late, excused and optional percentage. The response uses the same page envelope. The attendance policy and missing-data treatment are documented in FEATURE_REPORT_2026-10-07.md.

## File uploads (7 October 2026)

- POST `/api/v1/class-subjects/chapters/{id}/materials/upload`: multipart `file`, `title`, optional `description`; assigned teacher only; creates a FILE material (201).
- POST `/api/v1/assignments/{id}/submit-file`: multipart `file`, optional `answerText`; existing enrollment/deadline/grading/closed rules apply (200). Previous attachments stay in history.
- GET `/api/v1/files/{id}`: authenticated attachment download. Material: current subject visibility. Submission: owner with subject access or assigned teacher. No public links.

PDF/PNG/JPEG, maximum 10 MiB; matching extension/signature required. Invalid files return 400, oversized files 413, unauthorized scope 403. Returned URLs are API-relative (`/api/v1/files/{id}`); clients must send authentication to the configured EONE host. Ordinary external URLs remain compatible. Local upload storage is persistent only when the configured directory/volume is preserved.
# Announcement browsing

`GET /api/v1/announcements/page?scope=<uuid>&target=CLASSROOM|SUBJECT|DEPARTMENT&page=0&size=20&search=`

Returns the standard `content`, `page`, `size`, `totalElements`, `totalPages` envelope, newest first. Size is 1–100; search is at most 200 characters and matches title/body case-insensitively. Classroom and subject access follow scoped permissions. Department browsing requires membership in that department or its HOD role. Existing classroom list endpoint remains available for older clients; the mobile app uses bounded pages.
