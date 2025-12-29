# Short-Term Housing Search and Booking Platform (Haven)

This project is a platform for searching and booking short-term accommodations for tourists, similar to Airbnb. The application is built using microservices architecture, allowing users to search for available properties, make bookings with a payment flow, manage property listings with images, and leave reviews. The system is developed with Java, Spring Framework, and various technologies for microservice-based architecture.

---

## Table of Contents

1. [Features](#features)
2. [Technologies Used](#technologies-used)
3. [Modules](#modules)
4. [Prerequisites](#prerequisites)
5. [Installation Guide](#installation-guide)
6. [Usage Instructions](#usage-instructions)
7. [Testing](#testing)
8. [Contact](#contact)

---

## Features

1. **Microservice Architecture:**
   * Independent databases per microservice ensuring data isolation and security.
   * Supports both synchronous (REST API) and asynchronous (Kafka) inter-service communications.

2. **User Management:**
   * User registration, JWT-based authentication, and Role-based access control.
   * User profile management.

3. **Property Management:**
   * Detailed property listings with **Image Upload** capabilities.
   * Management of property features (amenities) and availability.
   * **Favorites (Wishlist):** Users can add properties to their favorites list.

4. **Booking & Payment System:**
   * Full booking lifecycle: `PENDING` -> `AWAITING_PAYMENT` -> `CONFIRMED`.
   * **Mock Payment Integration:** Simulation of payment processing workflow.
   * Real-time booking status updates.

5. **Review System:**
   * Verified feedback system ensuring reviews are from actual guests.
   * Reviews are enriched with user details (username) from the User Service.
   * Automatic property rating calculation.

6. **Notification Service:**
   * Asynchronous email notifications via Kafka messaging (e.g., Booking Confirmation).

7. **Service Discovery & Gateway:**
   * Eureka-based service registry for load balancing.
   * API Gateway as a unified entry point with configured CORS and routing.

8. **API Documentation:**
   * Comprehensive and interactive API documentation via Swagger.

9. **Containerization:**
    * Fully Dockerized services and infrastructure for simplified deployment.

---

## Technologies Used

* **Programming Language:** Java 21
* **Frameworks and Tools:** Spring Boot, Spring Security, Spring Data JPA, Spring Cloud (Gateway, Netflix Eureka, OpenFeign)
* **Database:** PostgreSQL, Redis (Caching)
* **Messaging:** Kafka
* **Containerization:** Docker, Docker Compose
* **Documentation:** Swagger (OpenAPI)
* **Testing:** JUnit 5, Mockito
* **Others:** Lombok, Maven, Liquibase

---

## Modules

1. **User Service:**
   * Handles registration, authentication, and user profile management.

2. **Property Service:**
   * Manages detailed property listings, image uploads, features, and user favorites.

3. **Booking Service:**
   * Manages bookings lifecycle, payment simulation, and recent booking history.
   * Communicates via Kafka with the notification service.

4. **Review Service:**
   * Manages property reviews and updates property ratings asynchronously.

5. **Notification Service:**
   * Sends asynchronous email notifications based on booking events.

6. **API Gateway:**
   * Unified entry point for secured API requests.

7. **Discovery Server:**
   * Central registry for microservice communication and discovery.

---

## Prerequisites

* Java 21+ installed
* Maven installed
* Docker and Docker Compose installed
* PostgreSQL (optional for manual local run without Docker)

---

## Installation Guide

1. **Clone the Repository:**

   ```bash
   git clone https://github.com/9jer/booking-app.git
   cd booking-app
   ```

2. **Build the Project:**
```bash
mvn clean install
```

3. **Set Up Database:**

Ensure PostgreSQL is running. Configure credentials in `application.yml` (for local run) or rely on `docker-compose.yml` (for Docker run).

4. **Run with Docker:**
```bash
docker-compose up --build
```

---

## Usage Instructions

### Accessing Services:

* **API Gateway:** `http://localhost:8080`
* **Swagger Documentation:** `http://localhost:8080/swagger-ui.html`

### Key Endpoints (Examples):

| Method | Endpoint                        | Access | Description |
| --- |---------------------------------| --- | --- |
| **Auth & User** |                                 |  |  |
| POST | `/api/v1/auth/sign-in`          | Public | User login (JWT retrieval) |
| GET | `/api/v1/users/profile`         | Authenticated | Get current user profile |
| **Properties** |                                 |  |  |
| GET | `/api/v1/properties`            | Public | Search properties |
| POST | `/api/v1/images`                | Authenticated | Upload property image |
| POST | `/api/v1/favorites/{id}`        | Authenticated | Add/Remove property from favorites |
| **Bookings** |                                 |  |  |
| POST | `/api/v1/bookings`              | Authenticated | Create a booking |
| POST | `/api/v1/bookings/{id}/payment` | Authenticated | Initiate payment (Mock) |
| GET | `/api/v1/bookings/recent`       | Authenticated | Get recent bookings |

Include JWT in headers for secured endpoints:

```http
Authorization: Bearer <your_jwt_token>
```

---

## Testing

* **Unit and Integration Tests:**
```bash
mvn test
```


* **Postman Collection:** A Postman collection with all API endpoints is available [here](https://drive.google.com/file/d/1pboq4NaACYUkLefBXNmnXLjvcOX0JzKF/view?usp=sharing).

---

## Contact

* Email: [vladimir.stxsevich@gmail.com](mailto:vladimir.stxsevich@gmail.com)
