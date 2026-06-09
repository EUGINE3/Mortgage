CREATE TABLE notification_logs (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    recipient_email VARCHAR(255),
    subject VARCHAR(500) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    correlation_id VARCHAR(100),
    trace_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_notification_logs_application_id ON notification_logs(application_id);
CREATE UNIQUE INDEX idx_notification_logs_idempotency
    ON notification_logs(application_id, event_type, correlation_id);
