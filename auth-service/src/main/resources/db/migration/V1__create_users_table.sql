CREATE TABLE users
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    uuid          VARCHAR(36)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    auth_provider VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    is_enabled    BOOLEAN      NOT NULL DEFAULT FALSE,
    is_locked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_uuid (uuid),
    UNIQUE KEY uk_users_email (email),
    INDEX idx_users_email (email),
    INDEX idx_users_uuid (uuid)
);