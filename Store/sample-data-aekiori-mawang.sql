USE delivery_store;

SET @now = '2026-04-10 10:00:00.000000';

-- rerun-safe cleanup (only this sample IDs)
DELETE FROM menu_options WHERE id BETWEEN 91001 AND 91020;
DELETE FROM menu_option_groups WHERE id BETWEEN 90001 AND 90020;
DELETE FROM menu_tags WHERE menu_id BETWEEN 30001 AND 30020;
DELETE FROM tags WHERE id BETWEEN 40001 AND 40020;
DELETE FROM menus WHERE id BETWEEN 30001 AND 30020;
DELETE FROM menu_groups WHERE id BETWEEN 20001 AND 20020;
DELETE FROM store_categories WHERE store_id = 10001;
DELETE FROM categories WHERE id BETWEEN 11001 AND 11020;
DELETE FROM store_hours WHERE store_id = 10001;
DELETE FROM store_holidays WHERE store_id = 10001;
DELETE FROM stores WHERE id = 10001;

-- 1) store
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
    10001,
    101,
    '애기오리 마왕치킨 산본점',
    'OPEN',
    0,
    17000,
    3000,
    'https://cdn.aekiori.dev/store/10001/logo.png',
    @now,
    @now
);

-- 2) categories + store_categories
INSERT INTO categories (id, name, created_at, updated_at) VALUES
(11001, '치킨', @now, @now),
(11002, '야식', @now, @now),
(11003, '맛집', @now, @now);

INSERT INTO store_categories (store_id, category_id, created_at) VALUES
(10001, 11001, @now),
(10001, 11002, @now),
(10001, 11003, @now);

-- 3) menu_groups
INSERT INTO menu_groups (id, store_id, name, display_order, created_at, updated_at) VALUES
(20001, 10001, '메인메뉴', 1, @now, @now),
(20002, 10001, '사이드', 2, @now, @now),
(20003, 10001, '음료', 3, @now, @now);

-- 4) menus
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
(
    30001,
    10001,
    20001,
    '마왕치킨',
    '기본 시그니처. 맵기 선택 가능.',
    19900,
    1,
    0,
    'https://cdn.aekiori.dev/store/10001/menu/30001.png',
    1,
    @now,
    @now
),
(
    30002,
    10001,
    20001,
    '마왕양념치킨',
    '달콤매콤 시그니처 양념.',
    21900,
    1,
    0,
    'https://cdn.aekiori.dev/store/10001/menu/30002.png',
    2,
    @now,
    @now
),
(
    30003,
    10001,
    20002,
    '치즈볼(5개)',
    '사이드 인기 메뉴.',
    4500,
    1,
    0,
    'https://cdn.aekiori.dev/store/10001/menu/30003.png',
    1,
    @now,
    @now
),
(
    30004,
    10001,
    20003,
    '콜라 1.25L',
    '기본 음료.',
    2500,
    1,
    0,
    'https://cdn.aekiori.dev/store/10001/menu/30004.png',
    1,
    @now,
    @now
);

-- 5) tags + menu_tags
INSERT INTO tags (id, name, created_at, updated_at) VALUES
(40001, '시그니처', @now, @now),
(40002, '매운맛', @now, @now),
(40003, '인기', @now, @now),
(40004, '사이드추천', @now, @now),
(40005, '음료', @now, @now);

INSERT INTO menu_tags (menu_id, tag_id, created_at) VALUES
(30001, 40001, @now),
(30001, 40002, @now),
(30001, 40003, @now),
(30002, 40002, @now),
(30002, 40003, @now),
(30003, 40004, @now),
(30004, 40005, @now);

-- 6) store_hours + store_holidays
INSERT INTO store_hours (
    id,
    store_id,
    day_of_week,
    open_time,
    close_time,
    is_closed,
    created_at,
    updated_at
) VALUES
(50001, 10001, 1, '11:00:00', '23:00:00', 0, @now, @now),
(50002, 10001, 2, '11:00:00', '23:00:00', 0, @now, @now),
(50003, 10001, 3, '11:00:00', '23:00:00', 0, @now, @now),
(50004, 10001, 4, '11:00:00', '23:00:00', 0, @now, @now),
(50005, 10001, 5, '11:00:00', '23:30:00', 0, @now, @now),
(50006, 10001, 6, '11:00:00', '23:30:00', 0, @now, @now),
(50007, 10001, 7, NULL, NULL, 1, @now, @now);

INSERT INTO store_holidays (
    id,
    store_id,
    holiday_date,
    reason,
    created_at,
    updated_at
) VALUES
(60001, 10001, '2026-12-25', '크리스마스 임시 휴무', @now, @now);

-- 7) menu option groups + options
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
(90001, 30001, '맵기 선택', 1, 0, 1, 1, 1, @now, @now),
(90002, 30001, '추가 선택', 0, 1, 0, 3, 2, @now, @now),
(90003, 30002, '맵기 선택', 1, 0, 1, 1, 1, @now, @now);

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
(91001, 90001, '순한맛', 0, 1, 1, @now, @now),
(91002, 90001, '보통맛', 0, 1, 2, @now, @now),
(91003, 90001, '매운맛', 500, 1, 3, @now, @now),
(91004, 90001, '핵매운맛', 1000, 1, 4, @now, @now),
(91005, 90002, '치즈추가', 2000, 1, 1, @now, @now),
(91006, 90002, '떡추가', 1500, 1, 2, @now, @now),
(91007, 90002, '소스추가', 500, 1, 3, @now, @now),
(91008, 90003, '보통맛', 0, 1, 1, @now, @now),
(91009, 90003, '매운맛', 500, 1, 2, @now, @now),
(91010, 90003, '핵매운맛', 1000, 1, 3, @now, @now);

-- quick check
SELECT id, name, status, min_order_amount, delivery_tip FROM stores WHERE id = 10001;
SELECT id, name, price FROM menus WHERE store_id = 10001 ORDER BY menu_group_id, display_order;
