Act as the lead engineer responsible for taking my existing EONE project
from its current working prototype/full-stack foundation to a polished,
production-quality, resume-ready application.

You are simultaneously acting as:

- Principal Software Engineer
- Senior Java/Spring Boot Engineer
- Senior Flutter Engineer
- PostgreSQL Database Architect
- API Architect
- Security Engineer
- DevOps Engineer
- QA/Test Engineer
- Senior Mobile Product Designer
- UI/UX Designer
- Design-System Architect
- Technical Documentation Engineer

Treat this repository as a real software product maintained by a
professional engineering team.

Do NOT treat it as a college CRUD project.

The objective is to finish EONE properly, improve existing code rather
than blindly replacing it, remove architectural weaknesses, complete
missing product workflows, produce a polished mobile experience, enforce
correct domain rules, optimize the database, add production-grade testing
and documentation, and leave the repository in a condition that I can
confidently show to recruiters and discuss in software-engineering
interviews.


======================================================================
1. CRITICAL RULE: UNDERSTAND THE EXISTING PROJECT BEFORE CHANGING IT
======================================================================

DO NOT immediately rewrite the repository.

Before modifying anything:

1. Read the entire repository structure.
2. Read the existing README and documentation.
3. Inspect the Flutter application.
4. Inspect the Spring Boot backend.
5. Inspect all Flyway migrations and PostgreSQL schema.
6. Inspect Docker configuration.
7. Inspect CI configuration.
8. Inspect tests.
9. Understand authentication.
10. Understand refresh-token handling.
11. Understand current RBAC/scoped authorization.
12. Understand all existing entities and relationships.
13. Understand the current mobile navigation.
14. Understand current API contracts.
15. Understand which features already work.
16. Identify partially implemented features.
17. Identify duplicated or poor-quality code.
18. Identify security vulnerabilities.
19. Identify database design/performance problems.
20. Identify UI/UX inconsistencies.
21. Identify missing test coverage.
22. Identify deployment blockers.

Do not replace working functionality without a concrete architectural
reason.

Do not silently change API contracts without updating every consumer.

Do not destroy or reset the database simply because migration work is
required.

Use proper Flyway migrations for schema evolution.


======================================================================
2. EXISTING TECHNOLOGY STACK
======================================================================

The project currently uses approximately this architecture:

MOBILE
- Flutter
- Dart
- Riverpod
- Dio
- flutter_secure_storage
- Material UI

BACKEND
- Java 21
- Spring Boot 3.x
- Spring Security
- Spring Data JPA / Hibernate
- JWT access tokens
- Refresh tokens
- Bean Validation
- Flyway
- OpenAPI / Swagger
- Actuator

DATABASE
- PostgreSQL

INFRASTRUCTURE
- Docker
- Docker Compose
- GitHub Actions

The existing technology stack should generally be retained.

Do NOT migrate frameworks simply because another framework is newer.

Upgrade dependencies only when there is a clear reason and compatibility
has been verified.

Avoid unnecessary dependencies.


======================================================================
3. PRODUCT VISION
======================================================================

EONE is a real college/academic-management mobile platform.

It must support realistic academic structures rather than a simplistic
ADMIN / TEACHER / STUDENT CRUD model.

Primary actors are:

- Student
- Subject Teacher
- Class Teacher
- HOD / Department Administrator

A single user may simultaneously have multiple responsibilities.

Example:

Dr. Sharma
|
|-- HOD
|   `-- Computer Science Department
|
|-- CLASS_TEACHER
|   `-- TY Computer Science - Division A
|
`-- SUBJECT_TEACHER
    |-- FY CS -> Programming
    |-- SY CS -> DBMS
    `-- TY CS -> Cloud Computing

DO NOT create separate accounts for each responsibility.

One identity can have multiple scoped roles.


======================================================================
4. ABSOLUTELY CRITICAL DOMAIN RULE: SCOPED RBAC
======================================================================

Authorization must be based on BOTH:

ROLE
+
RESOURCE SCOPE

Do not implement authorization only as:

if role == TEACHER

because that is insufficient.

Authorization needs to answer questions such as:

- Which department?
- Which classroom?
- Which class-subject?
- Which student?
- Which academic term?

Examples:

An HOD may manage their department.

An HOD must NOT automatically manage another department.

A Class Teacher may have classroom-level responsibilities.

Being a Class Teacher must NOT automatically give permission to grade
every subject taught in that classroom.

A Subject Teacher may manage only explicitly assigned class-subjects.

A Subject Teacher teaching DBMS in TY CS must not automatically manage:

- Operating Systems
- FY CS
- another department
- another teacher's assignment

A Student should access only resources connected to their active
enrollment.

Authorization must be enforced in the backend.

Never rely only on hiding buttons in Flutter.


======================================================================
5. MULTI-ROLE EXPERIENCE
======================================================================

The user experience must support one account holding multiple roles.

The application should not force users to repeatedly log into different
accounts.

The application should provide clear contextual navigation.

For example:

Current responsibility:

[ Department Admin ]
[ Class Teacher ]
[ Teaching ]

Teachers/HODs may have multiple classrooms:

FY CS
SY CS
TY CS
FY IT
etc.

Implement an intuitive ACTIVE CLASSROOM CONTEXT.

Example:

Active Classroom
----------------
TY Computer Science - A       v

When the context changes, classroom-specific information changes:

- students
- assignments
- attendance
- subjects
- announcements
- statistics
- learning content

Never mix FY/SY/TY data accidentally.

Persist the selected classroom locally when appropriate.

Context switching should feel deliberate and obvious.


======================================================================
6. SUBJECT TEACHER RESPONSIBILITIES
======================================================================

Subject Teacher functionality should include:

- Assigned classroom/subject visibility
- Subject overview
- Chapter management
- Learning materials
- Notes
- PDFs
- Links
- Videos
- Assignment creation
- Assignment publishing
- Assignment deadlines
- Student submissions
- Grading
- Feedback
- Subject attendance
- Subject announcements
- Subject statistics
- Pending grading count
- Submission statistics

A Subject Teacher should NOT gain unrelated classroom privileges.


======================================================================
7. CLASS TEACHER RESPONSIBILITIES
======================================================================

Class Teacher functionality should include:

- Classroom overview
- Student list
- Classroom announcements
- Overall attendance overview
- Low-attendance monitoring
- Assignment overview across subjects
- Student academic overview
- Classroom statistics
- Student activity overview
- Classroom-level communication
- Student concern/flag visibility where implemented

Class Teacher permissions should remain distinct from Subject Teacher
permissions.

Class Teacher != owner of every subject.


======================================================================
8. HOD / DEPARTMENT ADMIN RESPONSIBILITIES
======================================================================

The HOD is a department-scoped administrator.

Implement/refine:

- Department dashboard
- User approval
- User suspension
- Teacher management
- Student management
- Programs/streams
- Academic terms
- Academic years
- Classroom creation
- Subject creation
- Class-subject mapping
- Teacher assignment
- Class Teacher assignment
- Subject Teacher assignment
- Student enrollment
- Department statistics
- Audit history
- Academic structure management
- Pending account requests
- Department announcements where appropriate

An HOD may simultaneously be:

- HOD
- Class Teacher
- Subject Teacher

The UI and backend must support this correctly.


======================================================================
9. STUDENT EXPERIENCE
======================================================================

Student functionality should include:

- Personal dashboard
- Current classroom
- Subjects
- Chapters
- Learning materials
- Notes
- Videos
- Assignments
- Assignment deadlines
- Assignment submission
- File attachments
- Submission history
- Marks
- Teacher feedback
- Attendance
- Subject attendance percentage
- Announcements
- Notifications
- Profile
- Session management

Students must not see management actions for resources they do not own.


======================================================================
10. ACADEMIC DOMAIN MODEL
======================================================================

Review and properly model these concepts where required:

- User
- Department
- Program / Stream
- Academic Term
- Academic Year where appropriate
- Classroom
- Enrollment
- Subject
- ClassSubject
- RoleAssignment
- Chapter
- Material
- Assignment
- Submission
- AttendanceSession
- AttendanceRecord
- Announcement
- Notification
- DeviceToken
- RefreshToken
- AuditLog

Do not create duplicate concepts where an existing entity already models
the domain correctly.

Refactor only where required.


======================================================================
11. ASSIGNMENT SYSTEM
======================================================================

Turn assignments into a realistic lifecycle.

Support states/workflows such as:

- DRAFT
- PUBLISHED
- OPEN
- SUBMITTED
- LATE
- GRADED
- CLOSED

Use domain logic rather than trusting Flutter to calculate security-
sensitive state.

Teacher functionality:

- create assignment
- edit before/according to allowed lifecycle
- publish assignment
- set due date
- set marks
- review submissions
- identify missing submissions
- identify late submissions
- grade
- provide feedback
- view submission statistics

Student functionality:

- view assignment
- view deadline
- view submission state
- submit text
- upload attachments
- resubmit only if policy permits
- see submitted timestamp
- see marks
- see feedback

Example teacher metrics:

Students          32
Submitted         27
Not Submitted      5
Graded            21
Pending Grading    6
Late               3


======================================================================
12. ATTENDANCE SYSTEM
======================================================================

Attendance should be a meaningful feature.

Subject Teacher:

- create attendance session
- mark Present / Absent
- optionally support Late / Excused only if domain design justifies it
- prevent accidental duplicate sessions
- view attendance history

Student:

- subject-wise attendance
- percentage
- session history

Class Teacher:

- classroom attendance overview
- students below threshold
- aggregate attendance

HOD:

- classroom/department-level attendance metrics

Perform aggregates efficiently in SQL/JPA.

Do not fetch thousands of records into Java simply to calculate a count
that PostgreSQL can calculate efficiently.


======================================================================
13. REAL FILE UPLOADS
======================================================================

Replace URL-only workflows where necessary with actual file upload
support.

Teachers should be able to upload:

- PDFs
- documents
- images
- learning material attachments

Students should be able to upload assignment attachments.

Use object storage.

Recommended architecture:

Flutter
  |
multipart upload
  v
Spring Boot
  |
validated storage abstraction
  v
Object Storage

Possible providers:

- Cloudflare R2
- AWS S3
- Cloudinary
- another appropriate provider

Do not store large file binary data directly in PostgreSQL.

PostgreSQL should store metadata such as:

- storage key
- original filename
- URL if appropriate
- MIME type
- file size
- uploader
- created_at

Validate:

- file size
- MIME type
- extension where appropriate
- ownership
- authorization

Use a clean StorageService abstraction.

If production storage credentials are unavailable, implement the
integration architecture and a safe local development provider rather
than pretending an external provider works.


======================================================================
14. PUSH NOTIFICATIONS
======================================================================

Current in-app notifications should be expanded into real mobile push
notifications.

Use Firebase Cloud Messaging unless existing architecture provides a
better justified solution.

Store device tokens safely.

Trigger useful push notifications for events such as:

- new assignment
- upcoming deadline
- assignment graded
- new announcement
- new learning material
- account approved
- classroom/role assignment changes

Architecture:

Domain Event
   |
Notification Service
   |-- Persist in-app notification
   `-- Push through FCM

Push delivery failure must not roll back important academic business
transactions.

Use appropriate transaction boundaries / asynchronous delivery.

Do not put all Firebase logic directly into controllers.


======================================================================
15. AUTHENTICATION & ACCOUNT LIFECYCLE
======================================================================

Complete production-oriented authentication flows:

- login
- logout
- refresh-token rotation
- expired session handling
- secure token storage in Flutter
- account approval
- account suspension
- change password
- forgot password
- password reset
- disabled-account behavior

Registration workflow:

REGISTER
   |
PENDING
   |
HOD approval
   |
ACTIVE
   |
notification sent

Do not expose whether arbitrary email addresses exist where that causes
security/privacy concerns.

Use BCrypt or an appropriate strong password encoder.

Never log plaintext passwords, access tokens or refresh tokens.


======================================================================
16. DATABASE QUALITY TARGET: A+
======================================================================

Treat PostgreSQL as a production relational database.

Review every table and relationship.

Optimize the schema carefully.

Use proper:

- primary keys
- foreign keys
- NOT NULL constraints
- unique constraints
- check constraints where beneficial
- indexes
- composite indexes
- timestamps
- transaction boundaries
- normalization

Prevent invalid data at DATABASE level where possible.

Examples:

A student must not have duplicate active enrollment in the same
classroom.

A teacher must not accidentally receive duplicate identical scoped role
assignments.

A class-subject mapping must not be duplicated.

Refresh tokens must have appropriate uniqueness/revocation handling.

Create indexes based on actual query patterns.

Likely index candidates may include combinations involving:

- user_id
- department_id
- classroom_id
- class_subject_id
- assignment_id
- submission status
- notification user/read status
- attendance session
- enrollment
- created_at
- due_at

DO NOT add indexes blindly.

Every index has a write/storage cost.

Review query patterns first.


======================================================================
17. JPA / HIBERNATE QUALITY
======================================================================

Prevent common ORM problems.

Audit:

- N+1 queries
- accidental lazy initialization errors
- unnecessary EAGER relationships
- excessive cascade operations
- giant persistence contexts
- unnecessary entity serialization
- transaction boundaries

Prefer:

- DTOs
- projections
- repository queries
- fetch joins/entity graphs only where justified
- pagination
- explicit transactional service methods

Controllers should not expose JPA entities directly.

Do not enable Open Session in View as a shortcut for poor transaction
design.

Keep business transactions inside service boundaries.


======================================================================
18. PAGINATION
======================================================================

Do not return unlimited collections for resources that can grow.

Implement pagination for suitable endpoints including:

- users
- students
- assignments
- submissions
- notifications
- audit logs
- announcements

Use a consistent API response shape.

Example:

{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 326,
  "totalPages": 17
}

Set sensible maximum page sizes.


======================================================================
19. SEARCH AND FILTERING
======================================================================

Implement useful server-side filtering/search where needed.

Examples:

Students:
- name
- roll number if model supports it
- classroom
- program
- year
- attendance threshold

Assignments:
- classroom
- subject
- status
- date

Submissions:
- submitted
- late
- pending grading
- graded

Audit:
- actor
- action
- date range

Avoid downloading everything and filtering on the phone.


======================================================================
20. AUDIT LOGGING
======================================================================

Make the existing audit infrastructure meaningful.

Audit important actions such as:

- user approval
- user suspension
- role assignment
- role removal
- enrollment
- enrollment removal
- marks changes
- assignment changes/deletion
- attendance edits
- academic structure changes

Capture useful contextual information without storing passwords/tokens or
unnecessary sensitive data.

Provide an HOD-readable audit screen.

Example:

10:42
Dr Sharma approved Rahul Patil

10:35
Prof Joshi published DBMS Assignment 4

Yesterday
Dr Sharma assigned Prof Kumar to TY CS


======================================================================
21. API ARCHITECTURE
======================================================================

Keep controllers thin.

Target structure:

Controller
   |
Service / Use Case
   |
Domain rules / Authorization
   |
Repository
   |
PostgreSQL

Use DTOs for request/response objects.

Use consistent API naming.

Use validation annotations.

Return appropriate HTTP codes.

Implement consistent error responses.

Example:

{
  "status": 403,
  "code": "SUBJECT_ACCESS_DENIED",
  "message": "You are not assigned to this subject",
  "timestamp": "..."
}

Do not expose Java/Hibernate stack traces to the client.


======================================================================
22. SECURITY
======================================================================

Perform a security pass.

Review:

- JWT validation
- JWT expiry
- refresh-token rotation
- refresh-token revocation
- password hashing
- CORS
- authentication filters
- role authorization
- object-level authorization
- mass assignment vulnerabilities
- unsafe file uploads
- SQL injection protection
- excessive data exposure
- secrets
- logging
- API validation
- rate limiting on login/reset endpoints
- HTTPS readiness

Never commit:

- production secrets
- private keys
- service-account credentials
- API tokens

Use environment variables.

Make `.env.example` safe.

Protect every sensitive endpoint at backend level.


======================================================================
23. FLUTTER APPLICATION ARCHITECTURE
======================================================================

Refactor Flutter into a maintainable feature-oriented architecture where
appropriate.

A reasonable structure may look like:

lib/
  app/
  core/
    api/
    auth/
    config/
    errors/
    storage/
    theme/
    utils/
    widgets/
  features/
    auth/
    dashboard/
    classrooms/
    subjects/
    assignments/
    attendance/
    notifications/
    hod/
    profile/
  models/

Do not reorganize folders merely for aesthetics.

Refactor only when it improves maintainability.

Keep:

presentation
state management
API/data access
models/domain concerns

properly separated.

Do not put business logic inside giant Widget build() methods.


======================================================================
24. UI/UX MISSION — MANDATORY
======================================================================

Act as a Senior Mobile Product Designer, UI/UX Designer,
Design-System Architect, and Senior Flutter Developer.

Redesign/build the Flutter mobile application so that it looks and
behaves like a polished real-world production application in 2026.

DESIGN DIRECTION

Use:

Material 3 Expressive
+
modern native mobile UX
+
adaptive/responsive Flutter layouts
+
subtle Apple Liquid Glass-inspired visual effects where appropriate.

DO NOT blindly copy Google or Apple.

Create an original visual identity for EONE.

The final result must feel:

- modern
- premium
- professional
- clean
- expressive
- trustworthy
- responsive
- accessible
- polished
- consistent
- production-ready

It must NOT look like:

- an AI-generated demo
- a college project
- a generic Flutter template
- an admin dashboard squeezed into a phone
- excessive glassmorphism
- excessive gradients
- excessive shadows
- excessive rounded cards
- random colors
- overly futuristic UI


======================================================================
25. DESIGN SYSTEM
======================================================================

BEFORE redesigning screens, establish a reusable application design
system.

Create centralized design tokens for:

- colors
- typography
- spacing
- radius
- elevation
- borders
- opacity
- animation duration
- icon sizing
- component sizing

Use a consistent 4/8-point spacing system.

Use a restrained EONE brand palette.

Create semantic color roles for:

- success
- warning
- error
- information
- pending
- completed
- cancelled
- disabled

Support:

LIGHT THEME
+
DARK THEME

Use Material 3 color roles correctly.

Do not scatter hardcoded Color(...) values across widgets.


======================================================================
26. VISUAL STYLE
======================================================================

Use Material 3 Expressive principles:

- expressive typography
- meaningful scale
- strong hierarchy
- variable shapes
- intentional containment
- expressive but professional color
- purposeful motion
- emphasis on primary actions

Prefer:

- spacing
- typography
- tonal surfaces
- visual hierarchy

over endless card borders.

Use soft tonal surfaces.

Use subtle elevation.

Cards will often use approximately 16-24 px corner radius where suitable,
but NOT every section needs to be a rounded card.

Avoid unnecessary borders.

Use subtle gradients only for meaningful hero/branding surfaces.

Glass/translucent treatments may be used sparingly for:

- navigation
- floating controls
- bottom sheets
- overlays
- selected hero surfaces

Never apply glassmorphism everywhere.


======================================================================
27. TYPOGRAPHY
======================================================================

Create a clear hierarchy:

- Display
- Headline
- Title
- Body
- Label
- Caption

Important information must be scannable:

- deadlines
- attendance %
- marks
- pending work
- unread notifications
- counts

Avoid tiny text.

Respect system text scaling.

Avoid clipped layouts when text size increases.


======================================================================
28. RESPONSIVE / ADAPTIVE UI
======================================================================

Do not design for one phone size.

Support:

- small phones
- standard phones
- large phones
- tablets
- foldables where reasonable
- portrait
- landscape

Use:

- LayoutBuilder
- MediaQuery
- adaptive breakpoints
- responsive constraints

Do not hardcode entire screen widths/heights.

Respect:

- SafeArea
- cutouts
- system bars
- keyboard
- text scale
- orientation


======================================================================
29. NAVIGATION
======================================================================

Phone:

Use an elegant bottom navigation experience for major destinations.

Tablet/wide screens:

Adapt to NavigationRail or appropriate wider navigation.

Use contextual:

- app-bar actions
- menus
- bottom sheets
- FABs only where appropriate

Do not expose every action simultaneously.


======================================================================
30. EONE MOBILE INFORMATION ARCHITECTURE
======================================================================

Do not use the exact same navigation for every role when it harms UX.

Adapt destinations to user capabilities.

Example Student navigation:

Home
Classroom
Assignments
Notifications
Profile

Example Teacher:

Home
Classes
Teaching
Notifications
Profile

Example HOD:

Home
Department
Classes
Notifications
Profile

For a multi-role user, provide clean context/role switching without
turning the mobile app into an enterprise desktop sidebar.


======================================================================
31. REUSABLE COMPONENT LIBRARY
======================================================================

Build reusable components such as:

- AppScaffold
- AppTopBar
- ResponsiveNavigation
- BottomNavigation
- PrimaryButton
- SecondaryButton
- AppIconButton
- AppCard
- MetricCard
- SectionHeader
- Avatar
- UserListTile
- StatusChip
- FilterChip
- SearchField
- AppTextField
- AppDropdown
- DatePicker
- EmptyState
- ErrorState
- OfflineState
- LoadingState
- SkeletonLoader
- ConfirmationDialog
- AppBottomSheet
- AppSnackbar
- NotificationTile
- AssignmentTile
- ClassroomSwitcher
- RoleContextSwitcher
- InformationBanner

Do not duplicate the same UI implementation in ten screens.


======================================================================
32. DASHBOARD DESIGN
======================================================================

STUDENT DASHBOARD should prioritize:

- greeting/user context
- active classroom
- attendance
- pending assignments
- unread notifications
- upcoming deadlines
- recent materials
- announcements

Example hierarchy:

Hello, Prathmesh
TY Computer Science

Attendance       83%
Pending Work       3
Unread Alerts      2

Upcoming
DBMS Assignment       Tomorrow
OS Quiz               10 Oct

Recent Material
Normalization Notes
Deadlock Presentation


TEACHER DASHBOARD should prioritize:

- active classroom/context switcher
- assigned classes
- pending grading
- active assignments
- attendance actions
- upcoming work

HOD DASHBOARD should prioritize:

- department
- students
- teachers
- classrooms
- pending approvals
- department attendance
- recent administration activity

Do not simply show a collection of unrelated navigation cards.


======================================================================
33. LOADING / EMPTY / ERROR / OFFLINE STATES
======================================================================

Every major screen must deliberately support:

- normal
- loading
- empty
- error
- offline where relevant
- success
- disabled where relevant

Do not show blank screens.

Use useful empty states.

Example:

"No assignments due right now"

is better than:

"No data"

Use skeletons where appropriate rather than showing a blocking spinner
for every screen.


======================================================================
34. MICRO-INTERACTIONS
======================================================================

Use subtle meaningful motion:

- button feedback
- selection transitions
- bottom-sheet transition
- navigation transition
- expand/collapse
- skeleton loaders
- success feedback
- pull to refresh

Animations should feel quick and natural.

Do not animate merely because Flutter supports animation.


======================================================================
35. ACCESSIBILITY
======================================================================

The application must support:

- adequate contrast
- accessible touch targets
- screen readers
- Semantics
- dynamic text scaling
- focus states
- reduced-motion considerations
- keyboard navigation where relevant

Never communicate status using color alone.


======================================================================
36. ICONS AND GRAPHICS
======================================================================

Use one consistent icon language.

Prefer Material Symbols / suitable native icons.

Do not use emoji as interface icons.

Do not mix random outline/filled icon packs without reason.

Illustrations should only be used when genuinely helpful.


======================================================================
37. FORMS
======================================================================

All forms need:

- labels
- validation
- helpful errors
- proper keyboard type
- sensible autofill
- loading state
- submit-state protection
- server error presentation

Prevent duplicate submissions caused by repeated taps.

Date/time pickers should be appropriate to the platform.


======================================================================
38. FLUTTER ASYNC SAFETY
======================================================================

Resolve all analyzer issues.

For example, do not use BuildContext after an async gap unless guarded
appropriately.

Target:

flutter analyze

=> zero errors
=> ideally zero warnings/issues

Do not silence warnings with ignore comments unless a real documented
reason exists.


======================================================================
39. NETWORKING
======================================================================

Centralize API URL/configuration.

Never hardcode emulator-only addresses across the app.

Use environment/dart defines appropriately.

Endpoint handling must correctly support the backend base path.

Centralize:

- auth interceptor
- refresh handling
- timeout behavior
- error mapping
- request configuration

Never log auth tokens in production.

Provide clean API error translation for the UI.


======================================================================
40. OFFLINE / CONNECTION EXPERIENCE
======================================================================

Full offline-first architecture is not mandatory unless justified.

However, gracefully handle:

- no internet
- backend unavailable
- timeout
- retry
- cached user/context where safe

Do not crash or display raw Dio errors.


======================================================================
41. ERROR UX
======================================================================

Users should not normally see:

- DioException
- Java stack traces
- Hibernate errors
- HTTP 500
- raw SQL errors

Translate errors appropriately.

Examples:

Incorrect:
"DioException bad response 403"

Correct:
"You don't have permission to manage this subject."

Incorrect:
"500 Internal Server Error"

Correct:
"Something went wrong while loading this class. Try again."


======================================================================
42. PERFORMANCE
======================================================================

BACKEND:

Review:

- query count
- N+1
- pagination
- database indexes
- large transactions
- unnecessary serialization
- expensive aggregation
- connection pooling

Use EXPLAIN/EXPLAIN ANALYZE for important queries where appropriate.

FLUTTER:

Review:

- unnecessary rebuilds
- Riverpod provider scopes
- const usage
- huge widget trees
- expensive work inside build()
- image caching
- list virtualization
- pagination
- request duplication

Do not "optimize" based only on intuition.

Measure meaningful problems.


======================================================================
43. TESTING STRATEGY
======================================================================

The project is NOT finished if it only works manually.

BACKEND:

Use:

- JUnit 5
- Spring Boot Test
- MockMvc / appropriate HTTP integration testing
- Testcontainers PostgreSQL where practical

Test important authorization scenarios:

- student cannot create assignment
- student cannot grade
- teacher cannot grade another teacher's subject
- Subject Teacher cannot access unrelated class-subject
- Class Teacher cannot automatically edit other subjects
- HOD can manage own department
- HOD cannot manage unrelated department
- pending account cannot access protected APIs
- suspended user is blocked
- expired token is rejected
- refresh flow works
- duplicate enrollment fails
- duplicate role assignment fails
- invalid attendance operation fails
- assignment deadline rules work

Repository/database tests should use PostgreSQL behavior for database-
specific functionality rather than assuming H2 behaves identically.

FLUTTER:

Implement:

- unit tests
- provider/state tests
- widget tests
- important integration tests

Test:

- login
- error login
- dashboard
- role-aware navigation
- classroom switching
- assignment form
- submission
- notifications
- loading/error/empty states


======================================================================
44. CODE QUALITY STANDARD
======================================================================

Target A+ maintainability.

Requirements:

- descriptive names
- small cohesive classes/functions
- clear package boundaries
- SOLID where useful
- avoid unnecessary abstraction
- avoid God classes
- avoid giant controllers
- avoid giant service methods
- avoid giant Flutter build methods
- no copy/paste business logic
- no dead code
- no unexplained magic values
- no unused dependencies
- no TODO placeholders left in critical paths
- no commented-out production code
- no fake services pretending integration exists

Use comments to explain WHY, not obvious WHAT.

Do not over-engineer.

Readable code beats clever code.


======================================================================
45. LOGGING / OBSERVABILITY
======================================================================

Use structured, meaningful backend logs.

Log:

- application startup
- important failures
- security-relevant events where appropriate
- external integration failures

Do not log:

- passwords
- raw access tokens
- refresh tokens
- sensitive personal data unnecessarily

Expose proper health/readiness endpoints.

Prepare for production monitoring.


======================================================================
46. DOCKER
======================================================================

Maintain a reliable Docker development environment.

Docker Compose should support at least:

- backend
- PostgreSQL

Use health checks.

Use persistent PostgreSQL volumes.

Do not require developers to install PostgreSQL locally.

Never delete Docker volumes automatically during normal startup/shutdown.

Document:

docker compose up -d

and:

docker compose down

separately from destructive reset operations.


======================================================================
47. CI/CD
======================================================================

GitHub Actions should automatically verify:

BACKEND:
- compile
- tests
- packaging

FLUTTER:
- dependency resolution
- formatting check
- flutter analyze
- tests
- Android build verification where practical

CI should fail on real quality problems.

Do not configure CI to hide errors merely to make it green.

Later, prepare a deployment workflow if production credentials are
available.


======================================================================
48. PRODUCTION DEPLOYMENT
======================================================================

Prepare the project for realistic deployment.

Suggested architecture:

Flutter Android/iOS
        |
       HTTPS
        |
Spring Boot REST API
        |
   +----+---------+
   |              |
PostgreSQL     Object Storage
                  |
                  |
                 FCM

Suitable providers may include:

Backend:
- Railway
- Render
- equivalent

Database:
- Managed PostgreSQL

Storage:
- Cloudflare R2
- AWS S3
- Cloudinary

Push:
- Firebase Cloud Messaging

Do not hard-code localhost configuration into release builds.

Production must use HTTPS.

Use environment-specific configuration:

development
test
production

If credentials are required, stop only at the point where I must provide
them.

Do not fake deployment success.


======================================================================
49. README
======================================================================

Create a recruiter-quality README.

Include:

1. EONE title + concise description
2. Problem statement
3. Product overview
4. Main roles
5. Core workflows
6. Key features
7. Architecture
8. Technology stack
9. Security architecture
10. RBAC explanation
11. Database architecture
12. Screenshots
13. API overview
14. Docker setup
15. Local setup
16. Testing
17. Deployment
18. Environment configuration
19. Demo accounts where appropriate
20. Project structure
21. Key technical challenges
22. Future improvements

Do not write generic AI-style marketing text.

Make it technically credible.


======================================================================
50. ARCHITECTURE DOCUMENTATION
======================================================================

Maintain/update professional documentation:

- PRD
- Technical Requirements Document
- System Architecture
- Application Flow
- UI/UX specification
- Database schema
- ER diagram
- API documentation
- RBAC matrix
- Security documentation
- Testing strategy
- Deployment guide
- Implementation notes
- Traceability matrix
- Risks / technical debt
- Validation report


======================================================================
51. API DOCUMENTATION
======================================================================

Keep Swagger/OpenAPI accurate.

Document:

- authentication requirements
- request DTOs
- response DTOs
- error codes
- pagination
- important role permissions

Swagger must match actual behavior.


======================================================================
52. RESUME READINESS
======================================================================

At project completion, generate:

1. A 2-3 sentence recruiter explanation
2. A 30-second interview explanation
3. A 2-minute technical explanation
4. Resume bullets
5. Technical challenges solved
6. Architecture talking points
7. Security talking points
8. Database optimization talking points
9. Mobile architecture talking points
10. Testing talking points

Do NOT claim technologies/features that are not actually implemented.


======================================================================
53. FEATURES NOT TO ADD JUST FOR IMPRESSIVENESS
======================================================================

Do not add scope-creep features before the core product is excellent.

Do NOT prioritize:

- blockchain
- AI chatbot
- facial recognition
- AI attendance
- AI recommendations
- video conferencing
- complex social-network features
- random generative AI

unless every core workflow is already complete and there is a genuine
product reason.

Quality > feature count.


======================================================================
54. IMPLEMENTATION PROCESS
======================================================================

Follow this exact sequence.


-----------------------------
PHASE 0 — BASELINE
-----------------------------

Before changing code:

- inspect Git status
- understand current branch
- do not delete user changes
- record baseline
- run existing tests
- run backend build
- run flutter analyze
- run Flutter tests
- inspect Docker startup
- document current failures


-----------------------------
PHASE 1 — COMPLETE AUDIT
-----------------------------

Produce a concise audit containing:

- current architecture
- existing features
- missing features
- broken workflows
- code-quality issues
- security issues
- database issues
- UI/UX issues
- test gaps
- deployment gaps

Classify each:

CRITICAL
HIGH
MEDIUM
LOW


-----------------------------
PHASE 2 — IMPLEMENTATION PLAN
-----------------------------

Before large refactoring, create a dependency-aware implementation plan.

For each task specify:

- what changes
- why
- affected files/modules
- DB migration required?
- API changes?
- Flutter changes?
- tests required?
- risk


-----------------------------
PHASE 3 — CORE DOMAIN & RBAC
-----------------------------

Finish:

- roles
- scopes
- HOD behavior
- Class Teacher behavior
- Subject Teacher behavior
- Student access
- multi-role behavior
- classroom context
- authorization tests


-----------------------------
PHASE 4 — DATABASE & BACKEND
-----------------------------

Improve:

- constraints
- indexes
- migrations
- transactions
- queries
- pagination
- filtering
- API consistency
- validation
- error handling
- audit
- security


-----------------------------
PHASE 5 — CORE PRODUCT WORKFLOWS
-----------------------------

Finish:

- academic structure
- classroom management
- materials
- assignments
- submission lifecycle
- grading
- attendance
- announcements
- notifications
- profile/session flows


-----------------------------
PHASE 6 — FILE STORAGE + PUSH
-----------------------------

Implement:

- real file upload
- storage abstraction
- attachment metadata
- FCM/device tokens
- push notification service


-----------------------------
PHASE 7 — DESIGN SYSTEM
-----------------------------

Before redesigning individual screens:

- colors
- typography
- spacing
- shapes
- components
- responsive breakpoints
- themes
- accessibility rules
- motion rules


-----------------------------
PHASE 8 — MOBILE REDESIGN
-----------------------------

Redesign feature by feature.

Recommended order:

1. Splash/session bootstrap
2. Login
3. Registration
4. Student Home
5. Teacher Home
6. HOD Home
7. Navigation
8. Classroom switching
9. Classroom detail
10. Subjects
11. Chapters/materials
12. Assignments
13. Submission
14. Grading
15. Attendance
16. Notifications
17. HOD management
18. Profile/settings
19. Search/filter
20. Empty/error/loading/offline states


-----------------------------
PHASE 9 — TESTING & HARDENING
-----------------------------

Run:

backend tests
Flutter tests
integration tests
authorization tests
database tests
security review
performance review

Fix all failures.

Do not mark tests ignored simply to obtain green CI.


-----------------------------
PHASE 10 — PRODUCTION PREPARATION
-----------------------------

Prepare:

- Docker
- environments
- deployment
- HTTPS assumptions
- production secrets
- storage configuration
- Firebase configuration
- CI/CD
- release build


-----------------------------
PHASE 11 — FINAL AUDIT
-----------------------------

Perform final audits for:

- UX
- design consistency
- accessibility
- code quality
- security
- database
- performance
- tests
- API consistency
- deployment
- documentation


======================================================================
55. DEVELOPMENT BEHAVIOR
======================================================================

Work incrementally.

Do NOT rewrite everything in one enormous operation.

After each meaningful phase:

1. format code
2. compile backend
3. run backend tests
4. run Flutter analyze
5. run Flutter tests
6. validate migrations
7. verify Docker where relevant
8. verify API compatibility

If something breaks, fix it before moving on.

Do not stack 25 unverified changes on top of each other.


======================================================================
56. GIT RULES
======================================================================

If this repository is already under Git:

- do not run git init again
- do not erase history
- do not force push
- do not overwrite unrelated user modifications

Prefer logical commits such as:

feat(auth): complete password reset flow
feat(assignments): add attachment support
feat(attendance): add classroom attendance analytics
refactor(api): standardize paginated responses
feat(ui): introduce EONE design system
test(security): cover scoped role authorization

If a feature branch workflow already exists, respect it.


======================================================================
57. DO NOT CHEAT QUALITY CHECKS
======================================================================

Never solve an error by:

- disabling security
- globally permitting all endpoints
- turning off validation
- removing tests
- ignoring analyzer errors
- suppressing compiler warnings without cause
- enabling unsafe CORS "*"
- exposing Hibernate entities
- making every relationship EAGER
- setting ddl-auto=create
- deleting production data
- resetting migrations unnecessarily

Fix root causes.


======================================================================
58. IMPORTANT EXISTING PROJECT CONSTRAINTS
======================================================================

This is an existing working application.

Preserve working integration.

Do not regress:

- authentication
- PostgreSQL data
- Docker setup
- physical-device Flutter support
- JWT behavior
- scoped roles

Configuration must remain environment-driven.

Do not hardcode local machine ports into production source code.


======================================================================
59. ACCEPTANCE CRITERIA
======================================================================

EONE is NOT complete until all of the following are true:

[ ] Student workflow works end-to-end
[ ] Subject Teacher workflow works end-to-end
[ ] Class Teacher workflow works end-to-end
[ ] HOD workflow works end-to-end
[ ] Multi-role user works correctly
[ ] Classroom/context switching works
[ ] Cross-class data never leaks
[ ] Scoped RBAC is enforced backend-side
[ ] Assignments have realistic lifecycle
[ ] Assignment uploads work
[ ] Learning material uploads work
[ ] Grading works
[ ] Attendance works
[ ] Attendance analytics work
[ ] Announcements work
[ ] In-app notifications work
[ ] Push notifications work
[ ] Account approval works
[ ] Password/session lifecycle works
[ ] Search/filter works where necessary
[ ] Pagination is implemented
[ ] Audit logging is useful
[ ] UI has consistent design system
[ ] Light theme works
[ ] Dark theme works
[ ] Small phones work
[ ] Large phones work
[ ] Tablet layout is reasonable
[ ] Loading states exist
[ ] Empty states exist
[ ] Error states exist
[ ] Offline/network failure is handled gracefully
[ ] Flutter analyze is clean
[ ] Flutter tests pass
[ ] Backend tests pass
[ ] Authorization tests pass
[ ] Database migrations work from clean DB
[ ] Existing DB migration path works
[ ] Docker startup works
[ ] Swagger is correct
[ ] CI is green
[ ] Production configuration exists
[ ] README is professional
[ ] Documentation is current
[ ] No production secrets are committed
[ ] No critical TODO placeholders remain


======================================================================
60. FINAL DELIVERABLE
======================================================================

After implementation, give me a final engineering report containing:

1. WHAT WAS BUILT

2. WHAT WAS CHANGED

3. ARCHITECTURAL IMPROVEMENTS

4. DATABASE IMPROVEMENTS
   Include constraints/indexes/query optimizations.

5. SECURITY IMPROVEMENTS

6. UI/UX IMPROVEMENTS

7. TEST COVERAGE

8. PERFORMANCE IMPROVEMENTS

9. DEPLOYMENT STATUS

10. REMAINING TECHNICAL DEBT

11. EXACT COMMANDS TO RUN LOCALLY

12. EXACT COMMANDS TO TEST

13. DEMO ACCOUNTS

14. MANUAL QA CHECKLIST

15. INTERVIEW TALKING POINTS

16. RESUME BULLETS

17. SCREENSHOTS I SHOULD CAPTURE FOR GITHUB/RESUME

18. ANY CREDENTIAL/EXTERNAL-SERVICE SETUP I STILL NEED TO PERFORM


======================================================================
61. COMMUNICATION RULE
======================================================================

Do not ask me questions that can be answered by inspecting the repository.

Do not repeatedly ask permission for obvious implementation details.

Use engineering judgment.

Ask me only when a decision genuinely requires:

- credentials
- paid provider choice
- destructive data operation
- major product decision with multiple valid alternatives

When you discover an issue, do not merely describe it.

Investigate its root cause.

Fix it.

Add a test where reasonable to prevent regression.

Then continue.


======================================================================
62. FINAL QUALITY BAR
======================================================================

The final application should be something that could realistically be:

- installed on a real student's Android phone
- used by a teacher every day
- operated by an HOD
- demonstrated live to a recruiter
- reviewed by a senior engineer
- deployed publicly
- maintained by another developer

The code should communicate:

"This developer understands software engineering."

Not:

"This developer generated many files."

Prioritize correctness, maintainability, security, UX, data integrity and
real-world behavior over unnecessary complexity.

Begin now with PHASE 0 and PHASE 1.

DO NOT immediately start rewriting code.

First inspect the complete repository and return:

1. Current architecture
2. Current implemented functionality
3. What is already good and should be preserved
4. Bugs/issues found
5. Missing functionality
6. Database problems
7. Backend problems
8. Flutter problems
9. UI/UX problems
10. Security problems
11. Test gaps
12. Deployment gaps
13. Proposed target architecture
14. Ordered implementation plan

Then begin implementation systematically after that audit.