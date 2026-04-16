SET @table_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'outbox'
);
SET @ddl := IF(
    @table_exists = 1,
    'RENAME TABLE outbox TO payment_outbox',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS payment_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(36) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    status ENUM('INIT','PUBLISHED') NOT NULL DEFAULT 'INIT',
    partition_key VARCHAR(100),
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_payment_outbox_event_id (event_id),
    INDEX idx_status_created_at (status, created_at),
    INDEX idx_aggregate_type_aggregate_id (aggregate_type, aggregate_id),
    INDEX idx_partition_key_created_at (partition_key, created_at)
);

UPDATE payment_outbox
SET status = 'INIT'
WHERE status NOT IN ('INIT', 'PUBLISHED');

ALTER TABLE payment_outbox
    MODIFY COLUMN event_id VARCHAR(36) NOT NULL,
    MODIFY COLUMN event_type VARCHAR(50) NOT NULL,
    MODIFY COLUMN payload TEXT NOT NULL,
    MODIFY COLUMN status ENUM('INIT','PUBLISHED') NOT NULL DEFAULT 'INIT',
    MODIFY COLUMN created_at DATETIME NOT NULL;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_outbox'
      AND index_name = 'uk_outbox_event_id'
);
SET @ddl := IF(
    @index_exists = 1,
    'ALTER TABLE payment_outbox DROP INDEX uk_outbox_event_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_outbox'
      AND index_name = 'uk_payment_outbox_event_id'
);
SET @ddl := IF(
    @index_exists = 1,
    'ALTER TABLE payment_outbox DROP INDEX uk_payment_outbox_event_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment'
      AND column_name = 'amount'
);
SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE payment ADD COLUMN amount INT NULL AFTER user_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment'
      AND column_name = 'pg_type'
);
SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE payment ADD COLUMN pg_type VARCHAR(20) NULL DEFAULT ''KAKAOPAY'' AFTER status',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment'
      AND column_name = 'pg_transaction_id'
);
SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE payment ADD COLUMN pg_transaction_id VARCHAR(100) NULL AFTER pg_type',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment'
      AND column_name = 'failed_reason'
);
SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE payment ADD COLUMN failed_reason VARCHAR(200) NULL AFTER pg_transaction_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE payment
SET amount = COALESCE(amount, final_amount, total_amount, 0)
WHERE amount IS NULL;

UPDATE payment
SET user_id = 0
WHERE user_id IS NULL;

UPDATE payment
SET pg_type = COALESCE(pg_type, payment_method, 'KAKAOPAY')
WHERE pg_type IS NULL;

UPDATE payment
SET pg_transaction_id = COALESCE(pg_transaction_id, provider_payment_id)
WHERE pg_transaction_id IS NULL;

UPDATE payment
SET failed_reason = COALESCE(failed_reason, fail_reason)
WHERE failed_reason IS NULL;

UPDATE payment
SET status = CASE status
    WHEN 'INIT' THEN 'PENDING'
    WHEN 'PROCESSING' THEN 'PENDING'
    WHEN 'SUCCEEDED' THEN 'SUCCESS'
    WHEN 'CANCEL_REQUESTED' THEN 'FAILED'
    WHEN 'CANCELED' THEN 'FAILED'
    ELSE status
END;

ALTER TABLE payment
    MODIFY COLUMN user_id BIGINT NOT NULL,
    MODIFY COLUMN amount INT NOT NULL,
    MODIFY COLUMN status ENUM('PENDING','SUCCESS','FAILED') NOT NULL DEFAULT 'PENDING',
    MODIFY COLUMN pg_type VARCHAR(20) NOT NULL DEFAULT 'KAKAOPAY',
    MODIFY COLUMN created_at DATETIME NOT NULL,
    MODIFY COLUMN updated_at DATETIME NOT NULL;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment'
      AND column_name = 'store_id'
);
SET @ddl := IF(@column_exists = 1, 'ALTER TABLE payment DROP COLUMN store_id', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment'
      AND column_name = 'total_amount'
);
SET @ddl := IF(@column_exists = 1, 'ALTER TABLE payment DROP COLUMN total_amount', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment'
      AND column_name = 'used_point_amount'
);
SET @ddl := IF(@column_exists = 1, 'ALTER TABLE payment DROP COLUMN used_point_amount', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment'
      AND column_name = 'final_amount'
);
SET @ddl := IF(@column_exists = 1, 'ALTER TABLE payment DROP COLUMN final_amount', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment'
      AND column_name = 'provider_payment_id'
);
SET @ddl := IF(@column_exists = 1, 'ALTER TABLE payment DROP COLUMN provider_payment_id', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment'
      AND column_name = 'payment_method'
);
SET @ddl := IF(@column_exists = 1, 'ALTER TABLE payment DROP COLUMN payment_method', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment'
      AND column_name = 'fail_reason'
);
SET @ddl := IF(@column_exists = 1, 'ALTER TABLE payment DROP COLUMN fail_reason', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment'
      AND column_name = 'processed_at'
);
SET @ddl := IF(@column_exists = 1, 'ALTER TABLE payment DROP COLUMN processed_at', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
