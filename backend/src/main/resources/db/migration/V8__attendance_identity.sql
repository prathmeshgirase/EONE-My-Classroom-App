ALTER TABLE attendance_sessions ADD COLUMN session_key varchar(64);
CREATE UNIQUE INDEX uq_attendance_session_key ON attendance_sessions(class_subject_id,session_key) WHERE session_key IS NOT NULL;
