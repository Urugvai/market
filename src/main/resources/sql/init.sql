

CREATE TABLE market_user (
    id VARCHAR(36) not null,
    login VARCHAR(128) not null,
    login_date DATE not null,
    account  NUMERIC,

    PRIMARY KEY(id)
);

CREATE UNIQUE INDEX idx_user_login ON market_user (login);

CREATE TABLE market_item (
    id VARCHAR(36) not null,
    name VARCHAR(128) not null,
    description TEXT,
    price  NUMERIC not null,

    PRIMARY KEY(id)
);

CREATE UNIQUE INDEX idx_item_name ON market_item (name);

CREATE TABLE market_user_item (
    id VARCHAR(36) not null,
    user_id  VARCHAR(36) not null,
    item_id  VARCHAR(36) not null,

    PRIMARY KEY(id)
);