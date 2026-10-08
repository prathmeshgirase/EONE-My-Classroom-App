CREATE TABLE departments (
  id uuid PRIMARY KEY,
  code varchar(30) NOT NULL UNIQUE,
  name varchar(120) NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);

CREATE TABLE programs (
  id uuid PRIMARY KEY,
  department_id uuid NOT NULL REFERENCES departments(id),
  code varchar(30) NOT NULL,
  name varchar(120) NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  UNIQUE(department_id, code)
);

CREATE TABLE academic_terms (
  id uuid PRIMARY KEY,
  name varchar(60) NOT NULL UNIQUE,
  start_date date NOT NULL,
  end_date date NOT NULL,
  active boolean NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);

CREATE TABLE users (
  id uuid PRIMARY KEY,
  email varchar(190) NOT NULL UNIQUE,
  password_hash varchar(100) NOT NULL,
  first_name varchar(80) NOT NULL,
  last_name varchar(80) NOT NULL,
  phone varchar(30),
  account_type varchar(20) NOT NULL,
  status varchar(20) NOT NULL,
  department_id uuid REFERENCES departments(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);
CREATE INDEX idx_users_department_status ON users(department_id, status);

CREATE TABLE classrooms (
  id uuid PRIMARY KEY,
  program_id uuid NOT NULL REFERENCES programs(id),
  academic_term_id uuid NOT NULL REFERENCES academic_terms(id),
  year_level varchar(30) NOT NULL,
  division varchar(30) NOT NULL,
  name varchar(120) NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  UNIQUE(program_id, academic_term_id, year_level, division)
);

CREATE TABLE subjects (
  id uuid PRIMARY KEY,
  department_id uuid NOT NULL REFERENCES departments(id),
  code varchar(30) NOT NULL,
  name varchar(120) NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  UNIQUE(department_id, code)
);

CREATE TABLE class_subjects (
  id uuid PRIMARY KEY,
  classroom_id uuid NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
  subject_id uuid NOT NULL REFERENCES subjects(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  UNIQUE(classroom_id, subject_id)
);

CREATE TABLE role_assignments (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role varchar(30) NOT NULL,
  department_id uuid REFERENCES departments(id),
  classroom_id uuid REFERENCES classrooms(id),
  class_subject_id uuid REFERENCES class_subjects(id),
  active boolean NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);
CREATE INDEX idx_roles_user_active ON role_assignments(user_id, active);
CREATE INDEX idx_roles_department ON role_assignments(department_id, role, active);
CREATE INDEX idx_roles_classroom ON role_assignments(classroom_id, role, active);
CREATE INDEX idx_roles_class_subject ON role_assignments(class_subject_id, role, active);

CREATE TABLE enrollments (
  id uuid PRIMARY KEY,
  student_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  classroom_id uuid NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
  status varchar(20) NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  UNIQUE(student_id, classroom_id)
);
CREATE INDEX idx_enrollments_classroom_status ON enrollments(classroom_id, status);

CREATE TABLE chapters (
  id uuid PRIMARY KEY,
  class_subject_id uuid NOT NULL REFERENCES class_subjects(id) ON DELETE CASCADE,
  title varchar(180) NOT NULL,
  position integer NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  UNIQUE(class_subject_id, position)
);

CREATE TABLE learning_materials (
  id uuid PRIMARY KEY,
  chapter_id uuid NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
  title varchar(180) NOT NULL,
  type varchar(20) NOT NULL,
  url varchar(1000) NOT NULL,
  description text,
  created_by uuid NOT NULL REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);

CREATE TABLE assignments (
  id uuid PRIMARY KEY,
  class_subject_id uuid NOT NULL REFERENCES class_subjects(id) ON DELETE CASCADE,
  title varchar(180) NOT NULL,
  description text NOT NULL,
  due_at timestamptz NOT NULL,
  max_marks double precision NOT NULL,
  published boolean NOT NULL,
  created_by uuid NOT NULL REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);
CREATE INDEX idx_assignments_class_subject_due ON assignments(class_subject_id, due_at);

CREATE TABLE submissions (
  id uuid PRIMARY KEY,
  assignment_id uuid NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
  student_id uuid NOT NULL REFERENCES users(id),
  answer_text text,
  attachment_url varchar(1000),
  status varchar(20) NOT NULL,
  submitted_at timestamptz,
  marks double precision,
  feedback text,
  graded_by uuid REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  UNIQUE(assignment_id, student_id)
);

CREATE TABLE attendance_sessions (
  id uuid PRIMARY KEY,
  class_subject_id uuid NOT NULL REFERENCES class_subjects(id) ON DELETE CASCADE,
  session_date date NOT NULL,
  topic varchar(180),
  created_by uuid NOT NULL REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);

CREATE TABLE attendance_records (
  id uuid PRIMARY KEY,
  session_id uuid NOT NULL REFERENCES attendance_sessions(id) ON DELETE CASCADE,
  student_id uuid NOT NULL REFERENCES users(id),
  status varchar(20) NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  UNIQUE(session_id, student_id)
);

CREATE TABLE announcements (
  id uuid PRIMARY KEY,
  title varchar(180) NOT NULL,
  body text NOT NULL,
  target_type varchar(20) NOT NULL,
  department_id uuid REFERENCES departments(id),
  classroom_id uuid REFERENCES classrooms(id),
  class_subject_id uuid REFERENCES class_subjects(id),
  created_by uuid NOT NULL REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);

CREATE TABLE notifications (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title varchar(180) NOT NULL,
  body text NOT NULL,
  type varchar(30) NOT NULL,
  related_type varchar(40),
  related_id uuid,
  read_at timestamptz,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, read_at);

CREATE TABLE refresh_tokens (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash varchar(64) NOT NULL UNIQUE,
  expires_at timestamptz NOT NULL,
  revoked boolean NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);

CREATE TABLE audit_logs (
  id uuid PRIMARY KEY,
  actor_user_id uuid,
  action varchar(80) NOT NULL,
  entity_type varchar(80) NOT NULL,
  entity_id uuid,
  metadata text,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);
CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);
