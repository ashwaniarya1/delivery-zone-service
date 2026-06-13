CREATE TABLE restaurants (
    id                     VARCHAR(64)       PRIMARY KEY,
    name                   VARCHAR(255)      NOT NULL,
    latitude               DOUBLE PRECISION  NOT NULL,
    longitude              DOUBLE PRECISION  NOT NULL,
    delivery_radius_meters INTEGER           NOT NULL,
    -- longitude first: ST_MakePoint(x, y) = ST_MakePoint(lon, lat)
    location               GEOGRAPHY(POINT, 4326)
        GENERATED ALWAYS AS (
            ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
        ) STORED
);

CREATE TABLE delivery_groups (
    group_id               VARCHAR(80)       PRIMARY KEY,
    restaurant_count       INTEGER           NOT NULL,
    target_latitude        DOUBLE PRECISION  NOT NULL,
    target_longitude       DOUBLE PRECISION  NOT NULL,
    target_radius_meters   INTEGER           NOT NULL
);

CREATE TABLE group_members (
    group_id               VARCHAR(80)  NOT NULL
        REFERENCES delivery_groups(group_id) ON DELETE CASCADE,
    restaurant_id          VARCHAR(64)  NOT NULL
        REFERENCES restaurants(id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, restaurant_id)
);
