-- Do not repair ambiguous legacy assignments automatically. Invalid rows must
-- be reviewed by an administrator before this migration can succeed.
ALTER TABLE role_assignments ADD CONSTRAINT ck_role_scope CHECK (
  (role = 'HOD' AND department_id IS NOT NULL AND classroom_id IS NULL AND class_subject_id IS NULL)
  OR (role = 'CLASS_TEACHER' AND department_id IS NULL AND classroom_id IS NOT NULL AND class_subject_id IS NULL)
  OR (role = 'SUBJECT_TEACHER' AND department_id IS NULL AND classroom_id IS NULL AND class_subject_id IS NOT NULL)
);

CREATE UNIQUE INDEX uq_active_hod ON role_assignments(user_id, department_id)
  WHERE active AND role = 'HOD';
CREATE UNIQUE INDEX uq_active_class_teacher ON role_assignments(user_id, classroom_id)
  WHERE active AND role = 'CLASS_TEACHER';
CREATE UNIQUE INDEX uq_active_subject_teacher ON role_assignments(user_id, class_subject_id)
  WHERE active AND role = 'SUBJECT_TEACHER';

ALTER TABLE academic_terms ADD CONSTRAINT ck_term_dates CHECK (end_date > start_date);
ALTER TABLE assignments ADD CONSTRAINT ck_assignment_marks
  CHECK (max_marks > 0 AND max_marks < 'Infinity'::double precision);
ALTER TABLE submissions ADD CONSTRAINT ck_submission_marks
  CHECK (marks IS NULL OR (marks >= 0 AND marks < 'Infinity'::double precision));
ALTER TABLE chapters ADD CONSTRAINT ck_chapter_position CHECK (position > 0);

-- Repository lookups that previously scanned child tables.
CREATE INDEX idx_materials_chapter_created ON learning_materials(chapter_id, created_at DESC);
CREATE INDEX idx_attendance_subject_date ON attendance_sessions(class_subject_id, session_date DESC);
CREATE INDEX idx_attendance_student ON attendance_records(student_id);
CREATE INDEX idx_announcements_classroom_created ON announcements(classroom_id, created_at DESC);
