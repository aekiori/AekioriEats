CREATE TABLE IF NOT EXISTS store_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    user_id BIGINT,
    final_amount INT,
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED') NOT NULL,
    reject_reason VARCHAR(200),
    paid_at DATETIME(6),
    decided_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_store_orders_order_id UNIQUE (order_id),
    INDEX idx_store_orders_store_status_created_at (store_id, status, created_at)
);
