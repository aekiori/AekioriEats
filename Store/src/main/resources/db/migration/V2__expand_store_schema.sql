SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'stores'
      AND column_name = 'status_override'
);
SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE stores ADD COLUMN status_override BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'stores'
      AND column_name = 'min_order_amount'
);
SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE stores ADD COLUMN min_order_amount INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'stores'
      AND column_name = 'delivery_tip'
);
SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE stores ADD COLUMN delivery_tip INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'stores'
      AND column_name = 'store_logo_url'
);
SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE stores ADD COLUMN store_logo_url VARCHAR(512) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS store_categories (
    store_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (store_id, category_id),
    CONSTRAINT fk_store_categories_store
        FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_store_categories_category
        FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS menu_groups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_menu_groups_store_name UNIQUE (store_id, name),
    CONSTRAINT fk_menu_groups_store
        FOREIGN KEY (store_id) REFERENCES stores(id)
);

CREATE TABLE IF NOT EXISTS menus (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    menu_group_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    price INT NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    is_sold_out BOOLEAN NOT NULL DEFAULT FALSE,
    menu_image_url VARCHAR(512) NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_menus_store_name UNIQUE (store_id, name),
    CONSTRAINT fk_menus_store
        FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_menus_menu_group
        FOREIGN KEY (menu_group_id) REFERENCES menu_groups(id)
);

CREATE TABLE IF NOT EXISTS tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tags_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS menu_tags (
    menu_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (menu_id, tag_id),
    CONSTRAINT fk_menu_tags_menu
        FOREIGN KEY (menu_id) REFERENCES menus(id),
    CONSTRAINT fk_menu_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags(id)
);

CREATE TABLE IF NOT EXISTS store_hours (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    day_of_week TINYINT NOT NULL,
    open_time TIME NULL,
    close_time TIME NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_store_hours_store_day UNIQUE (store_id, day_of_week),
    CONSTRAINT fk_store_hours_store
        FOREIGN KEY (store_id) REFERENCES stores(id)
);

CREATE TABLE IF NOT EXISTS store_holidays (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    holiday_date DATE NOT NULL,
    reason VARCHAR(100) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_store_holidays_store_date UNIQUE (store_id, holiday_date),
    CONSTRAINT fk_store_holidays_store
        FOREIGN KEY (store_id) REFERENCES stores(id)
);

CREATE TABLE IF NOT EXISTS menu_option_groups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    menu_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_multiple BOOLEAN NOT NULL DEFAULT FALSE,
    min_select_count INT NOT NULL DEFAULT 0,
    max_select_count INT NOT NULL DEFAULT 1,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_menu_option_groups_menu_name UNIQUE (menu_id, name),
    CONSTRAINT fk_menu_option_groups_menu
        FOREIGN KEY (menu_id) REFERENCES menus(id)
);

CREATE TABLE IF NOT EXISTS menu_options (
    id BIGINT NOT NULL AUTO_INCREMENT,
    option_group_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    extra_price INT NOT NULL DEFAULT 0,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_menu_options_group_name UNIQUE (option_group_id, name),
    CONSTRAINT fk_menu_options_option_group
        FOREIGN KEY (option_group_id) REFERENCES menu_option_groups(id)
);

CREATE INDEX idx_stores_status ON stores (status);
CREATE INDEX idx_menus_store_id_menu_group_id ON menus (store_id, menu_group_id);
CREATE INDEX idx_menus_store_id_is_available ON menus (store_id, is_available, is_sold_out);
CREATE INDEX idx_menu_groups_store_id_display_order ON menu_groups (store_id, display_order);
CREATE INDEX idx_store_categories_category_id ON store_categories (category_id);
CREATE INDEX idx_menu_tags_tag_id ON menu_tags (tag_id);
CREATE INDEX idx_store_holidays_store_id_holiday_date ON store_holidays (store_id, holiday_date);
