# Short-Term Housing Search and Booking Platform

## Description

This project is a platform for searching and booking short-term accommodations for tourists, similar to Airbnb. The application is built using microservices architecture, and it allows users to search for available properties, make bookings, manage property listings, and leave reviews. The system is developed with Java, Spring Framework, and various technologies for microservice-based architecture.

### Key Features:
- User registration and authentication
- Property search and booking management
- Review system for guests and property owners
- Scalable microservice-based architecture
- API integration for user, property, and booking management
- JWT-based security for authorization and authentication

## Table of Contents

1. [Technologies Used](#technologies-used)
2. [Installation Guide](#installation-guide)
3. [Usage Instructions](#usage-instructions)
4. [Contact](#contact)

## Technologies Used

- **Java**: The main programming language used for implementing backend services.
- **Spring Framework**: Used for building the microservices, including Spring Boot, Spring Security, and Spring Data JPA.
- **PostgreSQL**: Relational database for storing user, property, and booking data.
- **Spring Cloud**: Used for managing microservices and communication between them.
- **JWT**: For secure user authentication and authorization.
- **Maven**: Build automation tool for Java projects.

## Installation Guide

Follow these steps to set up the project on your local machine.

1. **Clone the repository**:

   ```bash
   git clone https://github.com/9jer/booking-app.git
   cd your-repository
   ```

2. **Install dependencies**:

   Ensure that you have Java 11 or later installed. Then, run the following command to install dependencies using Maven:

   ```bash
   mvn install
   ```

3. **Set up PostgreSQL**:

   - Create a PostgreSQL database and configure the database connection in the `application.properties` file.
   - Example database configuration:

     ```properties
     spring.datasource.url=jdbc:postgresql://localhost:5432/yourdbname
     spring.datasource.username=yourusername
     spring.datasource.password=yourpassword
     ```

4. **Start the application**:

   You can run the application with the following command:

   ```bash
   mvn spring-boot:run
   ```

## Usage Instructions

1. **Access the Application**:

   Once the services are up and running, you can access the platform through your browser. The API is available at `http://localhost:8080`, and each service can be accessed individually based on its configuration (e.g., user service, booking service, etc.).

2. **Testing the Application**:

   The project includes integration tests for each service. You can run tests using Maven:

   ```bash
   mvn test
   ```

3. **API Endpoints**:

   - **POST** `/api/v1/auth/sign-up`: Register a new user
   - **POST** `/api/v1/sign-in`: Login and receive a JWT token
   - **GET** `/api/v1/properties`: Get available properties
   - **POST** `/api/v1/bookings`: Create a new booking
   - **GET** `/api/v1/reviews`: Get reviews for a property

   Make sure to include a valid JWT token in the headers when making requests to protected endpoints.

## Contact

- Email: vladimir.stxsevich@gmail.com
