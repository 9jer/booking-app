--liquibase formatted sql

--changeset 9jer:create-reviews-table
CREATE TABLE Reviews (
                         review_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                         property_id BIGINT NOT NULL,
                         user_id BIGINT NOT NULL,
                         rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
                         comment TEXT NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP
);