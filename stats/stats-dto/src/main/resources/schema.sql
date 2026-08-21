CREATE TABLE IF NOT EXISTS endpoint_hits
(
    id        BIGSERIAL PRIMARY KEY,
    app       VARCHAR(255)                NOT NULL,
    uri       VARCHAR(500)                NOT NULL,
    ip        VARCHAR(45)                 NOT NULL,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

