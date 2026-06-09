CREATE TABLE processed_events (
    id UUID PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    partition_id INT NOT NULL,
    offset_value BIGINT NOT NULL,
    application_id UUID,
    event_type VARCHAR(50),
    correlation_id VARCHAR(100),
    processed_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_processed_events_offset UNIQUE (topic, partition_id, offset_value)
);

CREATE INDEX idx_processed_events_application_id ON processed_events(application_id);
