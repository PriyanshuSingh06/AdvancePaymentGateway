# Advance Payment Gateway

A Spring Boot payment gateway prototype for creating, processing, retrying, refunding, and tracking payments with idempotency and webhook-based status updates.

This repository is still a backend prototype and not a production payment platform. It focuses on the core payment lifecycle and persistence flow rather than real provider integration or full security controls.

## What is implemented

- Payment creation with validation and `Idempotency-Key` support.
- Payment lookup by id and full list retrieval.
- Payment processing through a mock payment processor.
- Retry flow for failed payments.
- Refund flow for successful payments.
- Persistent payment attempts with attempt tracking and status history.
- Webhook event handling with signature verification.
- Duplicate webhook protection through persisted event IDs.
- Flyway migrations for idempotency, payment attempts, and webhook records.
- Structured API error handling for not-found, validation, and invalid transaction states.

## Current feature set

### Payment lifecycle

- `CREATED` → `PENDING` → `SUCCESS` or `FAILED`
- Failed payments can be retried.
- Successful payments can be refunded to `REFUNDED`.
- A payment has a generated `paymentReference` and an idempotency key for safe replays.

### Mock processor behavior

The project uses `MockPaymentProcessor` to simulate provider behavior:

- the first attempt for a payment fails
- the next attempt succeeds
- the processor generates a transaction reference like `MOCK-TXN-...`

This is intentionally a simulated gateway flow and not connected to a live payment provider.

## Tech stack

- Java 26
- Spring Boot 4.0.0
- Spring Web
- Spring Validation
- Spring Data JPA
- PostgreSQL
- Flyway
- Maven

## Project structure

```text
src/main/java/com/paymentgateway/
  controller/
    PaymentController.java
    WebhookController.java
  dto/
    PaymentRequest.java
    PaymentResponse.java
    PaymentWebhookRequest.java
  entity/
    Payment.java
    PaymentAttempt.java
    PaymentAttemptStatus.java
    PaymentMethod.java
    PaymentStatus.java
    WebhookEvent.java
  exception/
    GlobalExceptionHandler.java
    InvalidPaymentException.java
    PaymentNotFoundException.java
  repository/
    PaymentRepository.java
    PaymentAttemptRepository.java
    WebhookEventRepository.java
  service/
    MockPaymentProcessor.java
    PaymentProcessor.java
    PaymentProcessorResult.java
    PaymentService.java
    WebhookService.java
    WebhookSignatureService.java

src/main/resources/
  application.yml
  db/migration/
    V2__add_idempotency_key.sql
    V3__backfill_idempotency_key.sql
    V4__enforce_idempotency_key.sql
    V5__create_payment_attempts_table.sql
    V6__create_webhook_events_table.sql
```

## Prerequisites

- JDK 26
- Maven
- PostgreSQL database

## Configuration

Update the datasource and webhook secret values in `src/main/resources/application.yml` before running the app.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/paymentgateway
    username: your_db_user
    password: your_db_password
    driver-class-name: org.postgresql.Driver

payment:
  webhook:
    secret: your-webhook-secret
```

The app also expects Flyway to run migrations against the same PostgreSQL database.

## Run the application

From the project root:

```bash
mvn clean package
mvn spring-boot:run
```

The application runs on the default Spring Boot port:

```text
http://localhost:8080
```

## API endpoints

### Payment endpoints

Base path: `/api/payments`

#### Create a payment

```http
POST /api/payments
Header: Idempotency-Key: <unique-value>
Content-Type: application/json
```

Body example:

```json
{
  "amount": 49.99,
  "currency": "USD",
  "customerEmail": "customer@example.com",
  "paymentMethod": "CARD"
}
```

#### Get all payments

```http
GET /api/payments
```

#### Get payment by id

```http
GET /api/payments/{id}
```

#### Process a payment

```http
POST /api/payments/{id}/process
```

#### Retry a failed payment

```http
POST /api/payments/{id}/retry
```

#### Refund a successful payment

```http
POST /api/payments/{id}/refund
```

### Webhook endpoint

Base path: `/api/webhooks`

```http
POST /api/webhooks/payment
Header: X-Webhook-Signature: <hash>
Content-Type: application/json
```

The webhook payload is parsed into `PaymentWebhookRequest`, validated through a signature check, and then stored as a processed event to prevent duplicate handling.

## Request and validation rules

- `amount` must be greater than `0`
- `currency` is required
- `customerEmail` must be a valid email
- `paymentMethod` is required
- `Idempotency-Key` is required on payment creation
- Reusing an existing idempotency key returns the original payment

## Payment and attempt status values

### Payment status

```text
CREATED
PENDING
SUCCESS
FAILED
REFUNDED
```

### Payment attempt status

```text
CREATED
PROCESSING
SUCCESS
FAILED
```

## Database and migration notes

The project includes Flyway migrations for:

- adding and enforcing idempotency keys
- creating the payment attempts table
- creating the webhook events table

These files are under:

```text
src/main/resources/db/migration/
```

The current codebase assumes an existing PostgreSQL schema and validates against it, so a fresh empty database should be checked carefully before startup.

## Current limitations

This project is intentionally a backend prototype and does not yet include:

- real payment provider integration
- card or bank account processing
- production authentication and authorization
- full transaction reconciliation and dispute handling
- strong production security controls
- comprehensive automated unit/integration test coverage

## Verification

The project was checked with Maven and the build currently succeeds:

```bash
mvn test
```

This confirms the code compiles and the project currently passes its Maven test lifecycle in the workspace build state.

## Next improvements

- add a V1 initial schema migration for clean database setup
- add unit and integration tests for payment and webhook flows
- add stronger idempotency validation for conflicting payloads
- replace mock processor logic with a real gateway provider
- add authentication, authorization, and secure webhook validation for production use