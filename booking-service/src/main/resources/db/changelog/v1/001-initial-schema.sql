--liquibase formatted sql

--changeset 9jer:create-booking-tables
CREATE TABLE Booking (
                         booking_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                         user_id BIGINT NOT NULL,
                         property_id BIGINT NOT NULL,
                         check_in_date DATE NOT NULL,
                         check_out_date DATE NOT NULL,
                         status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP
);

CREATE TABLE Booking_History (
                                 history_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                                 booking_id BIGINT NOT NULL,
                                 status VARCHAR(50) NOT NULL,
                                 changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 FOREIGN KEY (booking_id) REFERENCES Booking(booking_id) ON DELETE CASCADE
);