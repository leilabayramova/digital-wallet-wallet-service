# Digital Wallet Wallet Service

## Overview

Wallet Service is the core financial microservice of the Digital Wallet System. It is responsible for wallet creation, balance management, deposits, wallet status control, money transfers, transaction history, idempotency protection, and safe concurrent balance updates.

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
- RabbitMQ
- Docker
- Lombok
- Validation
- Global Exception Handling
- Logging

## Main Responsibilities

- Create wallets for existing users
- Generate unique wallet numbers
- Manage wallet balance
- Deposit money into wallets
- Manage wallet status
- Transfer money between wallets
- Store transfer history
- Store transaction history
- Prevent duplicate transfer requests
- Protect balance updates during concurrent transfers
- Publish wallet and transfer events for notifications

## Business Logic

Wallet creation requires a valid user. Before creating a wallet, Wallet Service calls User Service through OpenFeign and checks whether the user exists.

Each wallet is created with:

- Initial balance `0`
- Unique wallet number
- Selected currency
- Default status `ACTIVE`

Deposit operations are allowed only for existing and active wallets. The deposit amount must be greater than `0`.

Money transfer is the main business logic of this service. During transfer, the system validates that:

- Sender wallet exists
- Receiver wallet exists
- Sender and receiver wallets are not the same
- Both wallets are `ACTIVE`
- Sender and receiver currencies match
- Transfer amount is greater than `0`
- Sender has enough balance
- Balance cannot become negative

For each successful transfer, two transaction records are created:

- `DEBIT` transaction for sender wallet
- `CREDIT` transaction for receiver wallet

The transfer process uses idempotency protection to prevent duplicate transfers and locking to prevent inconsistent balance updates during concurrent requests.

## Service Communication

### OpenFeign

Used for synchronous service-to-service communication.

```text
Wallet Service -> User Service
```

Used when creating a wallet to check if the user exists.

### RabbitMQ

Used for asynchronous event-driven communication.

Wallet Service publishes events such as:

```text
WALLET_CREATED
TRANSFER_COMPLETED
TRANSFER_FAILED
WALLET_BLOCKED
```

These events are consumed by Notification Service.

## Database

Wallet Service has its own PostgreSQL database:

```text
digital_wallet_wallet_db
```

Database schema changes are managed with Liquibase.

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
senderWalletId
receiverWalletId
amount
currency
status
idempotencyKey
failureReason
createdAt
completedAt
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
PENDING
COMPLETED
FAILED
```

### TransactionType

```text
DEBIT
CREDIT
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

### Deposit Money

```http
POST /api/wallets/{id}/deposit
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
```

Request:

```json
{
  "senderWalletId": 1,
  "receiverWalletId": 2,
  "amount": 50,
  "currency": "AZN",
  "idempotencyKey": "transfer-123456"
}
```

### Get Wallet Transactions

```http
GET /api/wallets/{walletId}/transactions
```

### Block Wallet

```http
PATCH /api/wallets/{id}/block
```

### Activate Wallet

```http
PATCH /api/wallets/{id}/activate
```

## Project Structure

```text
controller
service
repository
entity
dto
mapper
enums
client
exception
config
event
```

## Error Handling

The service uses global exception handling to return structured error responses.

Example:

```json
{
  "status": 404,
  "message": "Wallet not found with id: 1",
  "timestamp": "2026-07-10T16:30:00"
}
```

Handled cases include:

- Wallet not found
- User not found
- Invalid wallet operation
- Validation errors
- Duplicate transfer request
- Insufficient balance
- Inactive or blocked wallet

## Running Locally

### 1. Create database

```sql
CREATE DATABASE digital_wallet_wallet_db;
```

### 2. Configure application.yaml

```yaml
server:
  port: 9092

spring:
  application:
    name: digital-wallet-wallet-service

  datasource:
    url: jdbc:postgresql://localhost:5432/digital_wallet_wallet_db
    username: postgres
    password: your_password

  jpa:
    hibernate:
      ddl-auto: none
    open-in-view: false

  liquibase:
    change-log: classpath:changelog-master.yaml

clients:
  user-service:
    url: http://localhost:9091
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
- Auth Service
- Notification Service
- API Gateway

## Features

- Wallet creation
- Unique wallet number generation
- Balance management
- Deposit operation
- Wallet status control
- Wallet-to-wallet money transfer
- DEBIT and CREDIT transaction records
- Transaction history
- Idempotency protection
- Concurrent balance update protection
- OpenFeign communication with User Service
- RabbitMQ event publishing
- PostgreSQL database
- Liquibase migrations
- DTO and mapper layer
- Validation
- Global exception handling
- Logging
