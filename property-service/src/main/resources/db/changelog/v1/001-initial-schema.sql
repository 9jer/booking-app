--liquibase formatted sql

--changeset 9jer:create-properties-and-features-tables
CREATE TABLE Properties (
                            property_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                            owner_id BIGINT NOT NULL,
                            title VARCHAR(255) NOT NULL,
                            description TEXT NOT NULL,
                            location VARCHAR(255) NOT NULL,
                            average_rating DECIMAL(3, 2),
                            price_per_night DECIMAL(10, 2) NOT NULL,
                            capacity INT NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP
);

CREATE TABLE Property_Feature (
                                  feature_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                                  name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE Properties_Property_Feature (
                                             property_id BIGINT NOT NULL,
                                             feature_id BIGINT NOT NULL,
                                             PRIMARY KEY (property_id, feature_id),
                                             FOREIGN KEY (property_id) REFERENCES Properties(property_id) ON DELETE CASCADE,
                                             FOREIGN KEY (feature_id) REFERENCES Property_Feature(feature_id) ON DELETE CASCADE
);

CREATE TABLE Images (
                        image_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                        property_id bigint NOT NULL,
                        url VARCHAR(255) NOT NULL,
                        FOREIGN KEY (property_id) REFERENCES Properties(property_id) ON DELETE CASCADE
);

--changeset 9jer:insert-default-property-features
INSERT INTO Property_Feature (name) VALUES
                                        ('WiFi'), ('Pool'), ('Parking'), ('Air Conditioning'), ('Kitchen');