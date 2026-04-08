# CardDemo Java - Mainframe Modernization

A credit card transaction processing application migrated from COBOL/CICS/VSAM to Java 21 + Spring Boot + PostgreSQL.

## Overview

This project is a complete migration of the AWS CardDemo mainframe application. The original COBOL/CICS system processed credit card transactions via 3270 terminal screens and batch JCL jobs. This Java version exposes the same functionality through REST APIs and Spring Batch jobs.

### Migration Mapping

| COBOL Component | Java Component | Description |
|---|---|---|
| COSGN00C.cbl (CC00) | AuthController | User sign-on |
| COACTVWC.cbl (CAVW) | AccountController GET | Account view |
| COACTUPC.cbl (CAUP) | AccountController PUT | Account update |
| COCRDLIC.cbl (CCLI) | CardController GET /account | Card listing |
| COCRDUPC.cbl (CCUP) | CardController PUT | Card update |
| COTRN00C.cbl (CT00) | TransactionController GET | Transaction list |
| COTRN02C.cbl (CT02) | TransactionController POST | Add transaction |
| COUSR00-03C.cbl (CU00-03) | AdminController | User CRUD |
| CORPT00C.cbl (CR00) | ReportController | Report generation |
| CBTRN02C.cbl (batch) | PostTransactionJob | Daily transaction posting |
| CBACT04C.cbl (batch) | InterestCalculationJob | Interest calculation |
| CBTRN03C.cbl (batch) | TransactionReportJob | Transaction report |
| VSAM KSDS files | PostgreSQL tables | Data storage |
| BMS screen maps | OpenAPI/Swagger UI | User interface |
| RACF security | JWT + Spring Security | Authentication |

## Tech Stack

- **Java 21** + **Spring Boot 3.2.5**
- **Spring Data JPA** + **PostgreSQL** (production) / **H2** (development)
- **Spring Security** + **JWT** (replaces RACF)
- **Spring Batch** (replaces JCL batch jobs)
- **Flyway** database migrations
- **SpringDoc OpenAPI** (Swagger UI)
- **Gradle** build system

## Prerequisites

- Java 21+
- Gradle 8+ (or use the included wrapper)
- PostgreSQL 15+ (production) or H2 (development)

## Running Locally

### Development Mode (H2 in-memory database)

```bash
cd carddemo-java
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The application starts on `http://localhost:8080/api/v1`

### Swagger UI

Open `http://localhost:8080/api/v1/swagger-ui.html` to explore the API.

### Default Users (seeded via Flyway)

| User ID | Password | Type |
|---|---|---|
| USER0001 | password | Admin (A) |
| USER0002 | password | User (U) |

> Note: Passwords in seed data are BCrypt-encoded.

### Production Mode (PostgreSQL)

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=carddemo
export DB_USERNAME=postgres
export DB_PASSWORD=your_password

./gradlew bootRun --args='--spring.profiles.active=prod'
```

## Running Tests

```bash
./gradlew test
```

## Building

```bash
./gradlew clean build
```

The JAR file is produced at `build/libs/carddemo-java-1.0.0.jar`.

## API Endpoints

### Authentication
- `POST /api/v1/auth/login` - Sign in and get JWT token

### Accounts
- `GET /api/v1/accounts` - List accounts (paginated)
- `GET /api/v1/accounts/{id}` - View account details
- `PUT /api/v1/accounts/{id}` - Update account

### Cards
- `GET /api/v1/cards/{cardNum}` - View card details
- `GET /api/v1/cards/account/{acctId}` - List cards by account
- `PUT /api/v1/cards/{cardNum}` - Update card

### Transactions
- `GET /api/v1/transactions/{tranId}` - View transaction
- `GET /api/v1/transactions/account/{acctId}` - List by account
- `POST /api/v1/transactions` - Create transaction

### Customers
- `GET /api/v1/customers` - List customers
- `GET /api/v1/customers/{custId}` - View customer

### Administration (Admin role required)
- `GET /api/v1/admin/users` - List users
- `POST /api/v1/admin/users` - Create user
- `PUT /api/v1/admin/users/{userId}` - Update user
- `DELETE /api/v1/admin/users/{userId}` - Delete user

### Reports
- `GET /api/v1/reports/transactions/{acctId}` - Transaction report

## Project Structure

```
carddemo-java/
├── api-spec/              # OpenAPI specification
├── src/main/java/com/cardemo/
│   ├── batch/             # Spring Batch jobs (from JCL/COBOL batch)
│   ├── config/            # Security, OpenAPI configuration
│   ├── controller/        # REST controllers (from CICS transactions)
│   ├── dto/               # Request/response DTOs (from BMS maps)
│   ├── entity/            # JPA entities (from COBOL copybooks)
│   ├── exception/         # Global exception handling
│   ├── repository/        # Spring Data JPA repositories (from VSAM files)
│   ├── security/          # JWT token provider and filter
│   └── service/           # Business logic services (from COBOL programs)
├── src/main/resources/
│   ├── application.yml    # Base configuration
│   ├── application-dev.yml  # Dev profile (H2)
│   ├── application-prod.yml # Prod profile (PostgreSQL)
│   └── db/migration/      # Flyway SQL migrations
└── build.gradle           # Gradle build file
```
