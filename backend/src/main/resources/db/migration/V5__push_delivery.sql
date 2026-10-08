CREATE TABLE device_tokens (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    user_id uuid NOT NULL REFERENCES users(id),
    token varchar(512) NOT NULL UNIQUE
);
CREATE INDEX idx_device_tokens_user ON device_tokens(user_id);
ALTER TABLE notifications ADD COLUMN push_attempts integer NOT NULL DEFAULT 0;
ALTER TABLE notifications ADD COLUMN push_due_at timestamptz;
ALTER TABLE notifications ADD COLUMN push_delivered_at timestamptz;
CREATE INDEX idx_notification_push_due ON notifications(push_due_at) WHERE push_due_at IS NOT NULL;
