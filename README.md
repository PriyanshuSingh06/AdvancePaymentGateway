# Advance Payment Gateway

A Spring Boot REST API for managing payment records. This repository contains an **in-progress project**, not a production-ready payment gateway. It currently supports payment creation, lookup, simulated processing, and a basic refund state transition.

## Current Status

Implemented so far:

- Create payments with request validation and an `Idempotency-Key` header.
- Retrieve one payment or list all payments.
- Process payments through a mock processor.
- Refund successful payments by changing their status.
- Persist payment records with Spring Data JPA and PostgreSQL.
- Run Flyway migrations for adding and enforcing payment idempotency keys.
- Return structured errors for invalid requests, invalid payment transitions, and missing payments.

This is still under development. Payment processing is simulated; no payment provider, card handling, authentication, authorization, or production security controls are implemented. The mock processor rejects amounts greater than `100000` and accepts other amounts. A refund currently updates the payment status only; it does not issue money back through a provider.

## Technology

- Java 26 (configured in `pom.xml`)
- Spring Boot 4.0.0
- Spring Web, Validation, Spring Data JPA
- PostgreSQL
- Flyway
- Maven

## Getting Started

### Prerequisites

- JDK 26
- Maven
- PostgreSQL

### Configure the database

Create a PostgreSQL database named `paymentgateway` and provide credentials using environment variables:

```powershell
$env:DB_USERNAME = "your_database_user"
$env:DB_PASSWORD = "your_database_password"
```

The connection URL is currently configured as `jdbc:postgresql://localhost:5432/paymentgateway` in `src/main/resources/application.yml`.

> **Database setup is incomplete:** this repository currently contains Flyway migrations V2 through V4, but no V1 migration to create the initial `payments` table. V2 alters that table, so a fresh empty database does not yet have all migrations needed to start the application. The initial schema migration or an equivalent existing schema still needs to be added.

### Run the application

From the project root, with the database available and credentials set:

```bash
mvn spring-boot:run
```

The API uses Spring Boot's default port, `8080`.

Build the project with:

```bash
mvn clean package
```

## API

All endpoints are under `/api/payments`. Payment methods are `CARD`, `UPI`, `NET_BANKING`, and `WALLET`. Payment statuses are `CREATED`, `PENDING`, `SUCCESS`, `FAILED`, and `REFUNDED`.

### Create a payment

`POST /api/payments` requires an `Idempotency-Key` request header. Repeating a request with a previously used key returns the existing payment.

```bash
curl -X POST http://localhost:8080/api/payments \
	-H "Content-Type: application/json" \
	-H "Idempotency-Key: order-123-payment-1" \
	-d '{
		"amount": 49.99,
		"currency": "USD",
		"customerEmail": "customer@example.com",
		"paymentMethod": "CARD"
	}'
```

The amount must be greater than zero, the email must be valid, and all request fields are required.

### Retrieve payments

```text
GET /api/payments/{id}
GET /api/payments
```

### Process a payment

```text
POST /api/payments/{id}/process
```

Only payments in `CREATED` status can be processed. The mock processor changes the payment to `SUCCESS` or `FAILED`.

### Refund a payment

```text
POST /api/payments/{id}/refund
```

Only payments in `SUCCESS` status can be refunded. The current implementation changes the status to `REFUNDED` without calling an external service.

## Project Structure

```text
src/main/java/com/paymentgateway/
	controller/   REST endpoints
	dto/          Payment request and response models
	entity/       Payment persistence model and enums
	exception/    Domain exceptions and API error handling
	repository/   Spring Data repository
	service/      Payment workflow and mock processor
src/main/resources/
	application.yml
	db/migration/ Flyway migrations (V2-V4)
```

## Tests

The project includes Spring Boot's test dependency, but no test classes are currently present. Add tests as the implementation grows, especially for validation, idempotency, payment state transitions, and database migrations.

## Planned Work

- Add the initial database schema migration and verify clean-database startup.
- Add automated unit and integration tests.
- Define stronger idempotency behavior for key reuse with different request payloads.
- Integrate a real payment provider behind the `PaymentProcessor` interface.
- Implement provider-backed refunds, authentication, authorization, and operational safeguards before any production use.