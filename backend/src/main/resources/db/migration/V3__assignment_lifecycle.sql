ALTER TABLE assignments ADD COLUMN closed_at timestamptz;
ALTER TABLE assignments ADD CONSTRAINT ck_closed_assignment_published CHECK (closed_at IS NULL OR published);

CREATE TABLE submission_revisions (
  id uuid PRIMARY KEY,
  submission_id uuid NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
  answer_text text,
  attachment_url varchar(1000),
  submitted_at timestamptz NOT NULL,
  status varchar(20) NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);
CREATE INDEX idx_revision_submission_time ON submission_revisions(submission_id, submitted_at DESC);
