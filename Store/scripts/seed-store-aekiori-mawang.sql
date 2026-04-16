USE delivery_store;

SET @now := NOW(6);

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM menu_options WHERE option_group_id BETWEEN 90001 AND 90020;
DELETE FROM menu_option_groups WHERE id BETWEEN 90001 AND 90020;
DELETE FROM menu_tags WHERE menu_id BETWEEN 30001 AND 30020;
DELETE FROM tags WHERE id BETWEEN 40001 AND 40020;
DELETE FROM menus WHERE id BETWEEN 30001 AND 30020;
DELETE FROM menu_groups WHERE id BETWEEN 20001 AND 20020;
DELETE FROM store_holidays WHERE store_id = 3;
DELETE FROM store_hours WHERE store_id = 3;
DELETE FROM store_categories WHERE store_id = 3;
DELETE FROM categories WHERE id BETWEEN 11001 AND 11020;
DELETE FROM stores WHERE id = 3;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO stores (
    id,
    owner_user_id,
    name,
    status,
    status_override,
    min_order_amount,
    delivery_tip,
    store_logo_url,
    created_at,
    updated_at
) VALUES (
    3,
    101,
    'Aekiori Mawang Chicken',
    'OPEN',
    FALSE,
    15000,
    3000,
    'https://example.com/images/aekiori-mawang-chicken.png',
    @now,
    @now
);

INSERT INTO categories (id, name, created_at, updated_at) VALUES
    (11001, 'Chicken', @now, @now),
    (11002, 'Korean Food', @now, @now),
    (11003, 'Spicy', @now, @now)
ON DUPLICATE KEY UPDATE
    updated_at = VALUES(updated_at);

INSERT INTO store_categories (store_id, category_id, created_at) VALUES
    (3, (SELECT id FROM categories WHERE name = 'Chicken'), @now),
    (3, (SELECT id FROM categories WHERE name = 'Korean Food'), @now),
    (3, (SELECT id FROM categories WHERE name = 'Spicy'), @now);

INSERT INTO menu_groups (id, store_id, name, display_order, created_at, updated_at) VALUES
    (20001, 3, 'Main Menu', 1, @now, @now),
    (20002, 3, 'Side Menu', 2, @now, @now),
    (20003, 3, 'Drinks', 3, @now, @now);

INSERT INTO menus (
    id,
    store_id,
    menu_group_id,
    name,
    description,
    price,
    is_available,
    is_sold_out,
    menu_image_url,
    display_order,
    created_at,
    updated_at
) VALUES
    (30001, 3, 20001, 'Mawang Original Chicken', 'A crispy whole chicken with Aekiori mawang sauce.', 19900, TRUE, FALSE, 'https://example.com/images/menu/mawang-original.png', 1, @now, @now),
    (30002, 3, 20001, 'Mawang Boneless Chicken', 'Boneless chicken coated with sweet and spicy mawang sauce.', 21900, TRUE, FALSE, 'https://example.com/images/menu/mawang-boneless.png', 2, @now, @now),
    (30003, 3, 20001, 'Half Original Half Spicy', 'Half original crispy chicken and half spicy mawang chicken.', 22900, TRUE, FALSE, 'https://example.com/images/menu/half-half.png', 3, @now, @now),
    (30004, 3, 20002, 'Cheese Balls', 'Four chewy cheese balls.', 5500, TRUE, FALSE, 'https://example.com/images/menu/cheese-balls.png', 1, @now, @now),
    (30005, 3, 20002, 'French Fries', 'Crispy fried potatoes.', 4500, TRUE, FALSE, 'https://example.com/images/menu/fries.png', 2, @now, @now),
    (30006, 3, 20002, 'Pickled Radish', 'Chicken radish side dish.', 1000, TRUE, FALSE, NULL, 3, @now, @now),
    (30007, 3, 20003, 'Cola 1.25L', 'Large cola.', 3000, TRUE, FALSE, NULL, 1, @now, @now),
    (30008, 3, 20003, 'Cider 1.25L', 'Large cider.', 3000, TRUE, FALSE, NULL, 2, @now, @now);

INSERT INTO tags (id, name, created_at, updated_at) VALUES
    (40001, 'Best', @now, @now),
    (40002, 'Spicy', @now, @now),
    (40003, 'Boneless', @now, @now),
    (40004, 'Side', @now, @now),
    (40005, 'Drink', @now, @now);

INSERT INTO menu_tags (menu_id, tag_id, created_at) VALUES
    (30001, 40001, @now),
    (30001, 40002, @now),
    (30002, 40001, @now),
    (30002, 40003, @now),
    (30003, 40002, @now),
    (30004, 40004, @now),
    (30005, 40004, @now),
    (30007, 40005, @now),
    (30008, 40005, @now);

INSERT INTO store_hours (id, store_id, day_of_week, open_time, close_time, created_at, updated_at) VALUES
    (50001, 3, 1, '11:00:00', '23:30:00', @now, @now),
    (50002, 3, 2, '11:00:00', '23:30:00', @now, @now),
    (50003, 3, 3, '11:00:00', '23:30:00', @now, @now),
    (50004, 3, 4, '11:00:00', '23:30:00', @now, @now),
    (50005, 3, 5, '11:00:00', '00:30:00', @now, @now),
    (50006, 3, 6, '11:00:00', '00:30:00', @now, @now),
    (50007, 3, 7, '12:00:00', '23:00:00', @now, @now);

INSERT INTO store_holidays (id, store_id, holiday_date, reason, created_at, updated_at) VALUES
    (60001, 3, '2026-05-05', 'Children''s Day temporary holiday', @now, @now);

INSERT INTO menu_option_groups (
    id,
    menu_id,
    name,
    is_required,
    is_multiple,
    min_select_count,
    max_select_count,
    display_order,
    created_at,
    updated_at
) VALUES
    (90001, 30001, 'Spicy Level', TRUE, FALSE, 1, 1, 1, @now, @now),
    (90002, 30002, 'Spicy Level', TRUE, FALSE, 1, 1, 1, @now, @now),
    (90003, 30001, 'Extra Sauce', FALSE, TRUE, 0, 2, 2, @now, @now),
    (90004, 30002, 'Extra Sauce', FALSE, TRUE, 0, 2, 2, @now, @now);

INSERT INTO menu_options (
    id,
    option_group_id,
    name,
    extra_price,
    is_available,
    display_order,
    created_at,
    updated_at
) VALUES
    (91001, 90001, 'Mild', 0, TRUE, 1, @now, @now),
    (91002, 90001, 'Normal', 0, TRUE, 2, @now, @now),
    (91003, 90001, 'Hot', 500, TRUE, 3, @now, @now),
    (91004, 90002, 'Mild', 0, TRUE, 1, @now, @now),
    (91005, 90002, 'Normal', 0, TRUE, 2, @now, @now),
    (91006, 90002, 'Hot', 500, TRUE, 3, @now, @now),
    (91007, 90003, 'Mawang Sauce', 1000, TRUE, 1, @now, @now),
    (91008, 90003, 'Garlic Sauce', 1000, TRUE, 2, @now, @now),
    (91009, 90004, 'Mawang Sauce', 1000, TRUE, 1, @now, @now),
    (91010, 90004, 'Garlic Sauce', 1000, TRUE, 2, @now, @now);

SELECT 'stores' AS table_name, COUNT(*) AS row_count FROM stores WHERE id = 3
UNION ALL
SELECT 'menus', COUNT(*) FROM menus WHERE store_id = 3
UNION ALL
SELECT 'store_hours', COUNT(*) FROM store_hours WHERE store_id = 3
UNION ALL
SELECT 'menu_options', COUNT(*) FROM menu_options WHERE option_group_id BETWEEN 90001 AND 90020;
