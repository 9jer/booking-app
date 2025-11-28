--liquibase formatted sql

--changeset 9jer:create-review-indexes
CREATE INDEX idx_review_property_id ON Reviews(property_id);