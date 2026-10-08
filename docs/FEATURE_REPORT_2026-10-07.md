# Assignment and attendance increment — 7 October 2026

## Delivered

The login issue was confirmed resolved by the user. The exploratory login change and its test were reverted; this increment leaves login/session code unchanged.

Assignments now support private drafts, draft editing, publication and closing submissions. Teachers can list their drafts. Students and oversight-only staff cannot read drafts. Only the assigned subject teacher can edit, publish, close, grade or read submission history belonging to other students.

The mobile app includes a validated assignment editor, explicit deadline date/time selection, publish/close controls, lifecycle labels, teacher statistics, resubmission controls and a paginated previous-version screen. The grading flow is preserved.

`AssignmentLifecycleService` owns draft transitions and submission rules inside a transaction. Assignment-row locks serialize submission, grading and close operations. Previous ungraded answers are retained before replacement. Existing HTTP routes remain; new response fields (`closedAt`, `state`) are additive and mobile parsing is updated.

Teacher metrics are calculated in SQL for the current active enrollment roster: students, submitted, missing, graded, pending grading and late. Lateness remains visible after grading because it is calculated from submission timestamp versus deadline.

HODs and class teachers can open **Attendance overview** inside a classroom, page through students and filter below 75%. The backend validates the classroom-management scope and calculates counts, percentages, filtering and pagination in SQL. A subject-teacher responsibility alone does not expose classroom-wide academic records.

## Explicit domain policy

- Drafts may be edited; published instructions/deadline/marks are immutable in this increment.
- First submissions are accepted after the deadline while submissions remain open, and marked LATE.
- Revisions are permitted only before the deadline and before grading. Graded or closed work cannot be revised.
- Closing is idempotent and stops new submissions/revisions; grading existing work remains allowed. Reopening is not implemented.
- History contains earlier versions; the current version stays in the existing submission view.
- Attendance: PRESENT and LATE count as attended, ABSENT counts in the denominator, EXCUSED is excluded. Unmarked sessions do not count. No recorded denominator yields a missing percentage, not zero. The below-75 filter excludes students with no eligible records.
- Attendance summary pages contain at most 100 rows; history pages contain at most 100 versions. Other legacy list endpoints still need pagination.

## Database

V3 adds nullable `assignments.closed_at` and `submission_revisions`, with a foreign key, timestamp index and a check that only published assignments can be closed. V1 and V2 remain unchanged. Existing submissions are preserved; historical versions that were overwritten before this feature cannot be reconstructed.

Validation uses the isolated PostgreSQL 18 cluster on loopback port 55439. No existing application database, `.env`, or Docker volume was changed.

## Verification

- 23 backend tests pass against PostgreSQL with Flyway enabled and Hibernate validation.
- Two PostgreSQL migration tests pass from clean schemas and from V1 with existing data through V3.
- 10 Flutter tests pass, including draft validation, closed-work controls, graded feedback and server-side attendance filtering.
- Flutter analysis reports no issues; source is formatted.
- Android debug APK builds successfully at `mobile/build/app/outputs/flutter-apk/app-debug.apk`. Android tooling still emits the previously observed SDK XML version warning; signed release verification remains outstanding.
- GitHub-hosted CI, real-device end-to-end operation, Docker and public deployment remain unverified.

## Try the new workflows

1. Sign in as a subject teacher and open an assigned subject. Choose Create assignment, enter instructions/marks/deadline and save a draft.
2. Open the draft and edit it; publish when ready. A student enrolled in that classroom can now see it.
3. Submit as the student, revise before the deadline, and open Previous versions to see the prior answer.
4. As the assigned teacher, inspect counts and grade the submission. Verify that the student can see feedback but cannot revise graded work.
5. Close an assignment and verify that new submissions are rejected.
6. As HOD or class teacher, open Classroom → Attendance overview and toggle Below 75%.

## Still unfinished

Real uploads/object storage, push delivery/device registrations, password recovery and full account/session management, broad pagination/search, department-wide attendance reporting, student subject attendance percentages, audit UI, persistent classroom/role context, dark/adaptive design system, comprehensive accessibility and production deployment remain work. Duplicate attendance-session prevention and complete published-assignment amendment/reopening policy also remain. This is a verified increment, not a claim that the original full project request is complete.
