SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'store_hours'
      AND column_name = 'is_closed'
);

SET @ddl := IF(
    @column_exists = 1,
    'ALTER TABLE store_hours DROP COLUMN is_closed',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
