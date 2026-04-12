CREATE TABLE stores (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stores_owner_user_id_name UNIQUE (owner_user_id, name)
);

CREATE INDEX idx_stores_owner_user_id_status_created_at ON stores (owner_user_id, status, created_at);
