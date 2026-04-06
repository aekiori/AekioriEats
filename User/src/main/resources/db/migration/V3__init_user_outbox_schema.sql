CREATE TABLE outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(30) NOT NULL,
    partition_key VARCHAR(100),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_outbox_event_id UNIQUE (event_id)
);

CREATE INDEX idx_status_createdAt ON outbox (status, created_at);
CREATE INDEX idx_aggregateType_aggregateId ON outbox (aggregate_type, aggregate_id);
CREATE INDEX idx_partitionKey_createdAt ON outbox (partition_key, created_at);
