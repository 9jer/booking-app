--liquibase formatted sql

--changeset 9jer:create-booking-indexes
CREATE INDEX idx_booking_property_id ON Booking(property_id);
CREATE INDEX idx_booking_user_id ON Booking(user_id);
CREATE INDEX idx_booking_dates ON Booking(property_id, check_in_date, check_out_date);