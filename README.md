# Short-Term Housing Search and Booking Platform

This project is a platform for searching and booking short-term accommodations for tourists, similar to Airbnb. The application is built using microservices architecture, and it allows users to search for available properties, make bookings, manage property listings, and leave reviews. The system is developed with Java, Spring Framework, and various technologies for microservice-based architecture.

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

   * User registration and JWT-based authentication.

3. **Property Management:**

   * Detailed property listings.
   * Management of property features and availability.

4. **Booking Management:**

   * Automatic extraction of user ID from JWT for secure booking operations.
   * Real-time booking status updates (e.g., cancellation).

5. **Review System:**

   * Verified feedback system ensuring reviews are from actual guests.
   * Automatic verification of user-property interactions through bookings.

6. **Notification Service:**

   * Asynchronous email notifications via Kafka messaging.

7. **Rating System:**

   * Calculation and asynchronous updating of average property ratings.

8. **Service Discovery:**

   * Eureka-based service registry for load balancing and service discovery.

9. **API Documentation:**

   * Comprehensive and interactive API documentation via Swagger.

10. **Containerization:**

    * Fully Dockerized services and infrastructure for simplified deployment.

---

## Technologies Used

* **Programming Language:** Java
* **Frameworks and Tools:** Spring Boot, Spring Security, Spring Data JPA, Spring Cloud, OpenFeign
* **Database:** PostgreSQL
* **Messaging:** Kafka
* **Containerization:** Docker
* **Documentation:** Swagger (OpenAPI)
* **Others:** Lombok, Maven, Eureka

---

## Modules

1. **User Service:**

   * Handles registration, authentication, and user profile management.

2. **Property Service:**

   * Manages detailed property listings and property features.

3. **Booking Service:**

   * Manages bookings with real-time availability and status updates.
   * Communicates via Kafka with the notification service.

4. **Review Service:**

   * Manages property reviews, verifying authenticity through booking data.
   * Updates property ratings asynchronously.

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
* PostgreSQL (optional for manual verification)

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

Configure PostgreSQL in both `application.properties` and `application-docker.yml`:

**application.yml:**
```yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/yourdbname
    username: yourusername
    password: yourpassword
```

**application-docker.yml:**
```yml
spring:
  datasource:
    url: jdbc:postgresql://yourhostname:5432/yourdbname
    username: yourusername
    password: yourpassword
```

4. **Run with Docker:**

   ```bash
   docker-compose up --build
   ```

---

## Usage Instructions

### Accessing Services:

* **API Gateway:** `http://localhost:8080`
* **Swagger Documentation:** `http://localhost:8080/swagger-ui.html`

### Key Endpoints(Examples):

| Method | Endpoint                       | Access        | Description                      |
| ------ | ------------------------------ | ------------- | -------------------------------- |
| POST   | `/api/v1/auth/sign-up`         | Public        | User registration                |
| POST   | `/api/v1/auth/sign-in`         | Public        | User login (JWT token retrieval) |
| GET    | `/api/v1/properties`           | Public        | Retrieve properties              |
| POST   | `/api/v1/bookings`             | Authenticated | Create booking                   |
| PATCH  | `/api/v1/bookings/{id}/status` | Authenticated | Update booking status            |

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

* **Postman Collection:**  A Postman collection with all API endpoints is available [here](https://drive.google.com/file/d/11Fq5EUYMW6_s6hbG9hfi_DXSf28A1T5O/view?usp=sharing).

---

## Contact

* Email: [vladimir.stxsevich@gmail.com](mailto:vladimir.stxsevich@gmail.com)
