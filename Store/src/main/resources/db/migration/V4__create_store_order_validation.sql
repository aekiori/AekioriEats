CREATE TABLE IF NOT EXISTS store_order_validation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    result ENUM('ACCEPTED', 'REJECTED') NOT NULL,
    reject_code VARCHAR(50),
    reject_reason VARCHAR(200),
    validated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_store_order_validation_order_id ON store_order_validation (order_id);
