ALTER TABLE users ADD COLUMN credentials_changed_at timestamptz;
CREATE TABLE password_resets (
 id uuid PRIMARY KEY,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL,
 user_id uuid NOT NULL REFERENCES users(id),
 token_hash varchar(64) NOT NULL UNIQUE,
 expires_at timestamptz NOT NULL,
 used boolean NOT NULL DEFAULT false
);
CREATE INDEX idx_password_resets_user_created ON password_resets(user_id,created_at);
