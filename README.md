# Digital Wallet Wallet Service

## Overview

Wallet Service is the core financial microservice of the Digital Wallet System. It is responsible for wallet creation, balance management, top-ups, wallet status control, money transfers, transaction history, idempotency protection, and safe concurrent balance updates.

This service communicates with the User Service using OpenFeign to validate users before creating wallets.

User Service repository: [digital-wallet-user-service](https://github.com/leilabayramova/digital-wallet-user-service)

## Tech Stack

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- Liquibase
- OpenFeign
- Resilience4j (circuit breaker)
- Lombok
- Bean Validation
- springdoc-openapi (Swagger UI)
- JUnit 5 / Mockito
- Testcontainers (concurrency integration test)
- Global exception handling
- Logging (SLF4J)

## Main Responsibilities

- Create wallets for existing users
- Generate unique wallet numbers
- Manage wallet balance
- Top up money into wallets
- Manage wallet status (`ACTIVE` / `BLOCKED`)
- Transfer money between wallets
- Store transfer and transaction history
- Prevent duplicate transfer requests (idempotency)
- Protect balance updates during concurrent transfers

## Business Logic

Wallet creation requires a valid user. Before creating a wallet, Wallet Service calls User Service through OpenFeign and checks whether the user exists. This call is wrapped in a Resilience4j circuit breaker with a connect/read timeout — if User Service is slow or down, the request fails fast with `503` instead of hanging.

Each wallet is created with:

- Initial balance `0`
- Unique wallet number
- Selected currency
- Default status `ACTIVE`

Top-up operations are allowed only for existing and active wallets. The amount must be greater than `0`, and each top-up is recorded as a `TOP_UP` transaction.

Money transfer is the main business logic of this service. During transfer, the system validates that:

- Sender wallet exists
- Receiver wallet exists
- Sender and receiver wallets are not the same
- Both wallets are `ACTIVE`
- Sender and receiver currencies match
- Transfer amount is greater than `0`
- Sender has enough balance
- Balance cannot become negative (enforced both in code and with a DB `CHECK` constraint)

For each successful transfer, two transaction records are created:

- `DEBIT` transaction for sender wallet
- `CREDIT` transaction for receiver wallet

Wallets involved in a transfer are locked in a consistent order (by id) to avoid deadlocks under concurrent transfers.

### Idempotency

A transfer request can carry an `Idempotency-Key` header:

- Same key + same payload → the original result is returned, no duplicate transfer is created.
- Same key + a **different** payload (different wallets/amount) → rejected with `409 Conflict`.
- If two requests race with the same new key, the loser's wallet balance changes are rolled back and the winner's result is returned — balances are never double-applied.

## Service Communication

### OpenFeign

Used for synchronous service-to-service communication.

```text
Wallet Service -> User Service
```

Used when creating a wallet to check if the user exists. Wrapped with a circuit breaker and timeouts; returns `503` if User Service is unavailable.

### Planned (not implemented yet)

- RabbitMQ/Kafka event publishing (`WALLET_CREATED`, `TRANSFER_COMPLETED`, `TRANSFER_FAILED`, `WALLET_BLOCKED`) for a future Notification Service
- JWT-based ownership checks once Auth Service exists
- Transactional outbox pattern for reliable event delivery

## Database

Wallet Service has its own PostgreSQL database:

```text
digital_wallet_wallet_db
```

Database schema changes are managed with Liquibase. Money columns use `DECIMAL(19,4)`, and `wallets.balance` has a `CHECK (balance >= 0)` constraint as a second line of defense beyond the application-level checks.

## Main Entities

### Wallet

```text
id
userId
walletNumber
balance
currency
status
createdAt
updatedAt
```

### Transfer

```text
id
sourceWalletId
targetWalletId
amount
currency
status
idempotencyKey
requestHash
createdAt
```

### Transaction

```text
id
walletId
transferId
type
amount
balanceBefore
balanceAfter
createdAt
```

## Enums

### WalletStatus

```text
ACTIVE
BLOCKED
CLOSED
```

### TransferStatus

```text
COMPLETED
```

### TransactionType

```text
DEBIT
CREDIT
TOP_UP
```

## API Endpoints

### Create Wallet

```http
POST /api/wallets
```

Request:

```json
{
  "userId": 1,
  "currency": "AZN"
}
```

### Get Wallet By ID

```http
GET /api/wallets/{id}
```

### Top Up Wallet

```http
POST /api/wallets/{id}/top-up
```

Request:

```json
{
  "amount": 100
}
```

### Transfer Money

```http
POST /api/transfers
Idempotency-Key: transfer-123456   (optional)
```

Request:

```json
{
  "sourceWalletId": 1,
  "targetWalletId": 2,
  "amount": 50
}
```

### Get Wallet Transactions

```http
GET /api/wallets/{id}/transactions
```

### Block Wallet

```http
PATCH /api/wallets/{id}/block
```

### Activate Wallet

```http
PATCH /api/wallets/{id}/activate
```

### API Documentation

Once the service is running, Swagger UI is available at:

```text
http://localhost:9092/swagger-ui/index.html
```

## Project Structure

```text
controller
service
service/impl
repository
entity
dto
mapper
enums
client
exception
config
util
```

## Error Handling

The service uses global exception handling to return structured error responses.

Example:

```json
{
  "timestamp": "2026-07-10T16:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Wallet not found with id: 1",
  "validationErrors": null
}
```

Handled cases include:

- Wallet not found / user not found
- Invalid wallet status transition
- Validation errors (with a field-level `validationErrors` map)
- Duplicate transfer request / reused idempotency key with a different payload
- Insufficient balance
- Inactive or blocked wallet
- User Service unavailable (`503`)
- Data integrity constraint violations

## Running Locally

### 1. Create database

```sql
CREATE DATABASE digital_wallet_wallet_db;
```

### 2. Configure the local datasource password

`application.yaml` activates the `local` profile and does not contain a password. Create `src/main/resources/application-local.yaml` (already in `.gitignore`, never committed) with:

```yaml
spring:
  datasource:
    password: your_password
```

### 3. Run the application

```bash
./gradlew bootRun
```

Default port:

```text
9092
```

## Related Services

- [User Service](https://github.com/leilabayramova/digital-wallet-user-service)
- Auth Service (not started yet)
- Notification Service (not started yet)
- API Gateway (not started yet)

## Features

- Wallet creation with unique wallet number generation
- Balance management and top-up
- Wallet status control (block/activate)
- Wallet-to-wallet money transfer with full validation
- DEBIT / CREDIT / TOP_UP transaction ledger
- Idempotency protection with payload-hash validation
- Pessimistic locking with deadlock-safe lock ordering
- Resilience4j circuit breaker + timeouts around the User Service call
- PostgreSQL database with Liquibase migrations and DB-level balance constraint
- Swagger/OpenAPI documentation
- DTO and mapper layer, Bean Validation, global exception handling, logging
- Unit, controller, and Testcontainers-based concurrency tests
