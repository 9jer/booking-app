--liquibase formatted sql

--changeset 9jer:create-property-indexes
CREATE INDEX idx_property_price ON Properties(price_per_night);