CREATE TABLE IF NOT EXISTS compilations
(
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(50) NOT NULL UNIQUE,
    pinned      BOOLEAN     NOT NULL DEFAULT FALSE,
    description VARCHAR(1000)
);

CREATE TABLE IF NOT EXISTS compilation_events
(
    compilation_id BIGINT NOT NULL REFERENCES compilations (id) ON DELETE CASCADE,
    event_id       BIGINT NOT NULL,
    PRIMARY KEY (compilation_id, event_id)
);