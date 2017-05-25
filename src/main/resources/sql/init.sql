CREATE TABLE market_user (
    id VARCHAR(36) not null,
    login VARCHAR(128) not null,
    account NUMERIC,

    PRIMARY KEY(id)
);

CREATE UNIQUE INDEX idx_user_login ON market_user (login);

CREATE TABLE market_item_type (
    id VARCHAR(36) not null,
    name VARCHAR(128) not null,
    description TEXT,
    price  NUMERIC not null,

    PRIMARY KEY(id)
);

CREATE UNIQUE INDEX idx_item_type_name ON market_item_type (name);

CREATE TABLE market_item (
    id VARCHAR(36) not null,
    user_id  VARCHAR(36) not null,
    item_type_id  VARCHAR(36) not null,

    PRIMARY KEY(id)
);

ALTER TABLE market_item ADD CONSTRAINT fk_market_user_id FOREIGN KEY (user_id) REFERENCES market_user(id);

ALTER TABLE market_item ADD CONSTRAINT fk_market_item_id FOREIGN KEY (item_type_id) REFERENCES market_item_type(id);