SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment'
      AND column_name = 'provider_payment_id'
);
SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE payment ADD COLUMN provider_payment_id VARCHAR(100) NULL AFTER final_amount',
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
      AND column_name = 'payment_method'
);
SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE payment ADD COLUMN payment_method VARCHAR(30) NULL AFTER provider_payment_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE INDEX idx_payment_provider_payment_id ON payment (provider_payment_id);
