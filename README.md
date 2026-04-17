# Short-Term Housing Search and Booking Platform (Haven)

This project is a platform for searching and booking short-term accommodations for tourists, similar to Airbnb. The application is built using a modern microservices architecture, allowing users to search for available properties, make bookings with a payment flow, manage property listings with images, and leave reviews. The system is developed with Java 21, Spring Boot 3, and utilizes extensive technologies for messaging, observability, and container orchestration.

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
   * Synchronous communication via REST API (OpenFeign) and asynchronous event-driven communication via Kafka.
   * **Fault Tolerance:** Implemented using **Resilience4j Circuit Breaker** to handle downstream service failures gracefully.
   * Centralized cross-cutting concerns (Security, Exception Handling, Logging) in a shared library.

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
   * Asynchronous email notifications triggered by Kafka events (e.g., Booking Confirmation), strongly typed using **Avro** and **Schema Registry**.

7. **API Gateway:**
   * Spring Cloud Gateway acting as a unified entry point, handling CORS and routing requests to downstream microservices.

8. **Observability & Monitoring:**
   * Comprehensive observability stack for tracking distributed microservices.
   * **Metrics:** Collected via Prometheus.
   * **Distributed Tracing:** Tracing via OpenTelemetry (OTLP) and Tempo.
   * **Centralized Logging:** Logs aggregated using Promtail and Loki.
   * **Dashboards:** Visualizations and monitoring provided by Grafana.

9. **API Documentation:**
   * Comprehensive, aggregated, and interactive API documentation via Swagger (OpenAPI 3).

10. **Containerization & Orchestration:**
    * Fully Dockerized services for local development (`docker-compose`).
    * Production-ready **Kubernetes** manifests and Kustomize configurations for deployment (`k8s/` directory).

---

## Technologies Used

* **Programming Language:** Java 21
* **Frameworks and Tools:** Spring Boot 3, Spring Security, Spring Data JPA, Spring Cloud (Gateway, OpenFeign, Resilience4j)
* **Database:** PostgreSQL, Redis (Caching)
* **Messaging & Schemas:** Apache Kafka, Confluent Schema Registry, Apache Avro, Zookeeper, Kafka UI
* **Observability:** Prometheus, Grafana, Loki, Promtail, Tempo, OpenTelemetry (OTLP)
* **Containerization & Orchestration:** Docker, Docker Compose, Kubernetes, Kustomize
* **Documentation:** Swagger (OpenAPI 3)
* **Testing:** JUnit 5, Mockito, Testcontainers, WireMock, Spring Boot Test (@DataJpaTest, @SpringBootTest)
* **Others:** Lombok, Maven, MapStruct, Liquibase

---

## Modules

1. **common-lib:**
   * Centralized shared module containing global exception handling, security configurations, structured logging setup, and Feign client utilities.

2. **user-service:**
   * Handles user registration, JWT authentication, and user profile management.

3. **property-service:**
   * Manages detailed property listings, image uploads, property features, and user favorites.

4. **booking-service:**
   * Manages bookings lifecycle, payment simulation, and recent booking history. Emits Avro-serialized events via Kafka.

5. **review-service:**
   * Manages property reviews and updates property ratings asynchronously.

6. **notification-service:**
   * Consumes Avro messages from Kafka (validated via Schema Registry) to send asynchronous email notifications based on booking events.

7. **api-gateway:**
   * Unified, non-blocking entry point routing external requests to internal microservices and aggregating Swagger documentation.

---

## Prerequisites

* Java 21+ installed
* Maven installed
* Docker and Docker Compose installed
* (Optional) Kubernetes cluster (e.g., Minikube, Kind) for deploying K8s manifests
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

4. **Build and Run with Docker Compose (Local Environment):**

   This project uses the **Jib Maven Plugin** for containerization.
    
   **Step 1: Build local Docker images**
   First, build the microservices and load the images into your local Docker daemon by running the following command from the root directory:
   ```bash
   mvn clean compile jib:build
   ```
    
   **Step 2: Start the application**
   Once all images are successfully built, spin up the entire infrastructure in the background:
   ```bash
   docker-compose up -d
   ```

5. **Run in Kubernetes:**
   The project includes Kustomize configurations for deployment to Kubernetes. First, ensure you have a running cluster (e.g., Minikube).
   ```bash
   kubectl apply -k k8s/infra
   kubectl apply -k k8s/config
   kubectl apply -k k8s/apps
   ```

---

## Usage Instructions

### Accessing Key Services:

* **API Gateway (Main Entry Point):** `http://localhost:8080`
* **Swagger Documentation:** `http://localhost:8080/swagger-ui.html`
* **Grafana (Observability Dashboard):** `http://localhost:3000` (Default credentials: `admin` / `admin`)
* **Kafka UI:** `http://localhost:8090`

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
