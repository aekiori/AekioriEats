USE delivery_user;

SET @target_count = 3000000;
SET @batch_size = 100000;
SET autocommit = 1;

DROP TEMPORARY TABLE IF EXISTS seed_nums;
CREATE TEMPORARY TABLE seed_nums (
    n INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_nums (n)
SELECT
    ones.n
    + tens.n * 10
    + hundreds.n * 100
    + thousands.n * 1000
    + ten_thousands.n * 10000 AS n
FROM
    (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
    CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
    CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) hundreds
    CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) thousands
    CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ten_thousands;

DROP PROCEDURE IF EXISTS seed_users_3m;
DELIMITER $$
CREATE PROCEDURE seed_users_3m()
BEGIN
    DECLARE base INT DEFAULT 0;

    WHILE base < @target_count DO
        INSERT INTO users (
            email,
            status,
            created_at,
            updated_at
        )
        SELECT
            CONCAT('loadtest3m+', LPAD(base + n + 1, 7, '0'), '@example.com') AS email,
            CASE
                WHEN MOD(base + n + 1, 20) = 0 THEN 'LOCKED'
                WHEN MOD(base + n + 1, 33) = 0 THEN 'DEACTIVATED'
                ELSE 'ACTIVE'
            END AS status,
            NOW(6) - INTERVAL MOD(base + n + 1, 1440) MINUTE AS created_at,
            NOW(6) - INTERVAL MOD(base + n + 1, 1440) MINUTE AS updated_at
        FROM seed_nums
        WHERE base + n + 1 <= @target_count
        ON DUPLICATE KEY UPDATE
            updated_at = VALUES(updated_at);

        SET base = base + @batch_size;
    END WHILE;
END$$
DELIMITER ;

CALL seed_users_3m();
DROP PROCEDURE seed_users_3m;

SELECT COUNT(*) AS total_users FROM users;
