ALTER TABLE audit_logs ADD COLUMN department_id uuid REFERENCES departments(id);
CREATE INDEX idx_audit_department_created ON audit_logs(department_id, created_at DESC, id);
