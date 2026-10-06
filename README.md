# GameJoint API

The GameJoint API is the backend RESTful service for the GameJoint platform. This repository contains the server-side application logic, security configurations, and database integrations required to manage users, games, reviews, reports, and administrative moderation.

## Technology Stack and Architecture

The project is built using a modern Java ecosystem and follows a standard Model-View-Controller (MVC) architecture, enforcing a strict separation of concerns across controllers, services, repositories, and data transfer objects (DTOs).

### Core Framework
*   **Java**: The primary programming language.
*   **Spring Boot**: The foundational framework used to build the REST API. It handles dependency injection, auto-configuration, and the embedded web server.
*   **Maven**: Used for project build automation and dependency management, configured via `pom.xml` and utilizing the Maven Wrapper (`mvnw`) for environment consistency.

### Security and Authentication
*   **Spring Security**: Secures all API endpoints, managing routing permissions and Role-Based Access Control (RBAC).
*   **JSON Web Tokens (JWT)**: Implemented for stateless user authentication. The custom `JwtAuthenticationFilter` intercepts incoming HTTP requests to validate user tokens securely.
*   **OAuth2 Integration**: Facilitates third-party authentication flows, managed through dedicated Data Transfer Objects.
*   **Cloudflare Turnstile**: Integrated via the `TurnstileService` to provide bot protection and CAPTCHA validation.
*   **One-Time Passwords (OTP)**: Used for secure account recovery and email verification workflows.
*   **Rate Limiting**: A custom `RateLimitingService` protects the API from brute-force attacks and abuse by throttling excessive endpoint requests.

### Database and Persistence
*   **MariaDB**: The primary relational database engine used for robust and scalable data persistence.
*   **Spring Data JPA**: Acts as the abstraction layer over Hibernate/JPA, handling CRUD operations without boilerplate SQL.
*   **JPA Specifications**: Utilized in `GameSpecification.java` to construct dynamic, programmatic database queries for filtering and searching the game catalog.
*   **Entity Models**: Java classes mapped directly to database tables (e.g., `User`, `Game`, `Review`, `Report`, `LinkedAccount`, `Platform`, `Genre`).

### Additional Services and Features
*   **Global Exception Handling**: A `@ControllerAdvice` driven `GlobalExceptionHandler` intercepts runtime exceptions to return consistent, standardized HTTP error responses.
*   **Communication Services**: The `EmailService` and `LoginNotificationService` handle asynchronous alerting for account activity, security notifications, and recovery processes.
*   **Content Moderation**: Dedicated controllers and services (`ModerationController`, `ReportService`) allow administrators to monitor user reports, manage bans, and maintain platform integrity.
