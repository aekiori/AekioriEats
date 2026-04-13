CREATE TABLE IF NOT EXISTS outbox (
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
    CONSTRAINT uk_outbox_event_id UNIQUE (event_id),
    INDEX idx_status_created_at (status, created_at),
    INDEX idx_aggregate_type_aggregate_id (aggregate_type, aggregate_id),
    INDEX idx_partition_key_created_at (partition_key, created_at)
);
