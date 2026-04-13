CREATE TABLE IF NOT EXISTS payment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id BIGINT,
    store_id BIGINT,
    total_amount INT,
    used_point_amount INT,
    final_amount INT,
    status VARCHAR(30) NOT NULL,
    fail_reason VARCHAR(200),
    processed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_order_id UNIQUE (order_id),
    INDEX idx_payment_status_created_at (status, created_at)
);
