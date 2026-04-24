CREATE TABLE order_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(30) NOT NULL,
    to_status VARCHAR(30) NOT NULL,
    reason VARCHAR(500),
    source_type VARCHAR(50) NOT NULL,
    event_id VARCHAR(100),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_order_status_history_order_id_created_at
    ON order_status_history (order_id, created_at);
