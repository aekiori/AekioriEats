CREATE TABLE point_balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    balance INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_point_balance_user_id (user_id)
);

CREATE TABLE point_ledger (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    amount INT NOT NULL,
    type ENUM('DEDUCT','CHARGE','REFUND','EARN') NOT NULL,
    result ENUM('SUCCESS','FAILED') NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    reason VARCHAR(200),
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_point_ledger_idempotency_key (idempotency_key),
    INDEX idx_point_ledger_user_created_at (user_id, created_at),
    INDEX idx_point_ledger_order_id (order_id)
);

CREATE TABLE point_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(36) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    status ENUM('INIT','PUBLISHED') NOT NULL DEFAULT 'INIT',
    partition_key VARCHAR(100),
    created_at DATETIME NOT NULL,
    INDEX idx_status_created_at (status, created_at),
    INDEX idx_aggregate_type_aggregate_id (aggregate_type, aggregate_id),
    INDEX idx_partition_key_created_at (partition_key, created_at)
);
