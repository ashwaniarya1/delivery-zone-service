CREATE INDEX idx_restaurants_location ON restaurants USING GIST (location);
