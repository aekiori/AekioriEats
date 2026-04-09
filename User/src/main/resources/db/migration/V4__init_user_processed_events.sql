CREATE TABLE processed_events (
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    schema_version INT NOT NULL,
    occurred_at DATETIME(6) NULL,
    processed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (event_id)
);

CREATE INDEX idx_processed_events_aggregate_id ON processed_events (aggregate_id);
CREATE INDEX idx_processed_events_processed_at ON processed_events (processed_at);
