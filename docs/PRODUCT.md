# Product specification

## Goal

EONE models a college department where administration, class responsibility and subject teaching overlap without becoming the same permission. It is intended to replace chat groups, scattered file links and manual assignment/attendance tracking with one structured application.

## Personas

### HOD
Department-scoped administrator. Manages people and academic structure. May separately hold class-teacher and subject-teacher assignments.

### Class Teacher
Responsible for a classroom/cohort. Can see its roster, subjects and class-level activity, and may publish classroom announcements. Subject editing/grading still requires a subject-teacher assignment.

### Subject Teacher
Assigned to one specific `classroom + subject` pair. Owns chapters, materials, assignments, grading and subject attendance for that pair.

### Student
Belongs to a classroom through an enrollment. Consumes material, submits work and sees only personal submission/attendance state.

## Functional requirements implemented

- Pending registration and HOD approval.
- Department, program, academic-term, classroom and subject structure.
- FY/SY/TY/fourth-year/other year levels.
- Multi-classroom and multi-subject staff responsibilities.
- Explicit classroom context switching in mobile UI.
- Chapter and learning-material management.
- Assignment publishing, submission and grading.
- Subject attendance.
- Department/classroom/subject announcement model.
- Persistent notification inbox with unread state.
- Audit records for sensitive academic/admin actions.
- JWT access + refresh lifecycle.

## Intentionally externalized integrations

The following require credentials/accounts owned by the deployer and therefore are not hard-coded into the repository:

- Firebase/APNs OS-level push delivery.
- S3/R2/Supabase Storage or similar binary-file storage.
- SMTP/transactional email.
- Institutional SSO.

The core application remains functional locally without those services; material and submission attachments currently accept URLs.
