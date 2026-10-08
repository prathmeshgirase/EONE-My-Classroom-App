CREATE TABLE stored_files (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    owner_id uuid NOT NULL REFERENCES users(id),
    chapter_id uuid REFERENCES chapters(id),
    assignment_id uuid REFERENCES assignments(id),
    storage_key varchar(255) NOT NULL UNIQUE,
    filename varchar(255) NOT NULL,
    content_type varchar(255) NOT NULL,
    byte_size bigint NOT NULL CHECK (byte_size > 0 AND byte_size <= 10485760),
    CHECK ((chapter_id IS NOT NULL) <> (assignment_id IS NOT NULL))
);
CREATE INDEX idx_stored_files_owner ON stored_files(owner_id);
CREATE INDEX idx_stored_files_chapter ON stored_files(chapter_id);
CREATE INDEX idx_stored_files_assignment ON stored_files(assignment_id);
