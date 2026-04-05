CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    delivery_address VARCHAR(500) NOT NULL,
    total_amount INT NOT NULL,
    used_point_amount INT NOT NULL,
    final_amount INT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash VARCHAR(100),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_idempotency_key UNIQUE (idempotency_key)
);

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    menu_name VARCHAR(100) NOT NULL,
    unit_price INT NOT NULL,
    quantity INT NOT NULL,
    line_amount INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

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

CREATE INDEX idx_userId_createdAt ON orders (user_id, created_at);
CREATE INDEX idx_userId_status_createdAt ON orders (user_id, status, created_at);
CREATE INDEX idx_orderId ON order_items (order_id);
CREATE INDEX idx_status_createdAt ON outbox (status, created_at);
CREATE INDEX idx_aggregateType_aggregateId ON outbox (aggregate_type, aggregate_id);
CREATE INDEX idx_partitionKey_createdAt ON outbox (partition_key, created_at);
