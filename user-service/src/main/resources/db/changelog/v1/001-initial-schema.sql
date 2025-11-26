--liquibase formatted sql

--changeset 9jer:create-users-and-roles-tables
CREATE TABLE Users (
                       user_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                       username VARCHAR(30) NOT NULL UNIQUE,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       name VARCHAR(100) NOT NULL,
                       phone VARCHAR(20) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP
);

CREATE TABLE Roles (
                       role_id INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                       name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE users_roles (
                             user_id BIGINT NOT NULL,
                             role_id INT NOT NULL,
                             PRIMARY KEY (user_id, role_id),
                             FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
                             FOREIGN KEY (role_id) REFERENCES Roles(role_id) ON DELETE CASCADE
);

--changeset 9jer:insert-initial-roles
INSERT INTO Roles (name) VALUES ('ROLE_GUEST'), ('ROLE_OWNER'), ('ROLE_ADMIN');

--changeset 9jer:insert-mock-users
INSERT INTO Users (username, email, password, name, phone, created_at) VALUES
                                                                           ('guest_user', 'guest.user@example.com', '$2a$10$VsRKFaWgMJkEAmEzk0taN.T8WQ8sot6UqDdXd3NAXILNVTfT3zpfK', 'Guest User', '+1234567890', CURRENT_TIMESTAMP),
                                                                           ('owner_user', 'owner.user@example.com', '$2a$10$VsRKFaWgMJkEAmEzk0taN.T8WQ8sot6UqDdXd3NAXILNVTfT3zpfK', 'Owner User', '+1234567891', CURRENT_TIMESTAMP),
                                                                           ('admin_user', 'admin.user@example.com', '$2a$10$VsRKFaWgMJkEAmEzk0taN.T8WQ8sot6UqDdXd3NAXILNVTfT3zpfK', 'Admin User', '+1234567892', CURRENT_TIMESTAMP);

INSERT INTO users_roles(user_id, role_id) VALUES (1, 1), (2, 2), (3, 3);