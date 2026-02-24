# Utility Account API — Spring Boot Backend

## Overview
Production-grade REST API for utility account management with mobile money payment integration (M-Pesa, MTN Mobile Money, Airtel). Built as a portfolio project to demonstrate fintech backend architecture.

**Author:** Oualid Gharach  
**Target role:** Engineering Lead / Tech Lead  
**Status:** Phase 6C complete, Phase 6D (JWT auth) next

---

## Tech Stack
- Java 21
- Spring Boot 3.5.9
- Spring Data JPA + Hibernate
- PostgreSQL 16
- Liquibase (database migrations)
- Lombok
- SpringDoc OpenAPI (Swagger UI)
- Testcontainers (PostgreSQL image for integration tests)
- Maven

---

## Architecture Decisions
- **Luhn algorithm** for customer IDs (8-digit) and account numbers (10-digit)
- **UUID v7** for payment receipt numbers (time-sortable)
- **Soft delete** for providers (deactivate/reactivate pattern)
- **API key authentication** for payment providers (hashed, prefix stored)
- **Service layer pattern** with clear separation of concerns
- **Global exception handler** returning structured `validationErrors` map
- **Correlation ID filter** for request tracing
- **CORS** configured for `http://localhost:4200`

---

## Database Schema
PostgreSQL database: `utility_account`

### Tables
- `customers` — customer profiles with Luhn-based IDs
- `accounts` — utility accounts (main + secondary) linked to customers
- `payment_providers` — external payment providers with hashed API keys
- `payments` — payment records with UUID v7 receipt numbers

---

## API Endpoints

### Customers — `/api/v1/customers`
| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Onboard customer |
| GET | `/` | List all customers |
| GET | `/{id}` | Get customer by ID |
| GET | `/search?mobile=` | Search by mobile number |
| PUT | `/{id}` | Update customer |
| DELETE | `/{id}` | Delete customer |

### Accounts — `/api/v1/accounts`
| Method | Path | Description |
|--------|------|-------------|
| POST | `/customers/{id}/payments` | Make payment to main account |
| POST | `/{accountNumber}/payments` | Make payment to specific account |

### Providers — `/api/v1/providers`
| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Onboard provider (returns API key once) |
| GET | `/` | List all providers |
| GET | `/{id}` | Get provider by ID |
| PATCH | `/{id}` | Update provider name |
| DELETE | `/{id}` | Deactivate provider (soft delete) |
| POST | `/{id}/reactivate` | Reactivate provider |
| POST | `/{id}/regenerate-key` | Regenerate API key |

### Reports — `/api/v1/reports`
| Method | Path | Description |
|--------|------|-------------|
| GET | `/summary?from=&to=` | Global payment summary + provider breakdown |
| GET | `/payments?accountNumber=&customerId=&providerCode=&from=&to=` | Search payments |
| GET | `/accounts/{accountNumber}/summary` | Account lifetime totals |
| GET | `/providers/{providerCode}/reconciliation?from=&to=` | Provider settlement report |

---

## DTOs

### Request DTOs
- `CreateCustomerRequest` — firstName, lastName, email, mobileNumber (all required)
- `UpdateCustomerRequest` — same fields, all optional (partial update)
- `CreateProviderRequest` — code, name
- `UpdateProviderRequest` — name only

### Response DTOs
- `CustomerSummary` — customerId, firstName, lastName, email, mobileNumber
- `CustomerDetailed` — above + accounts list + createdAt + updatedAt
- `ProviderResponse` — id, code, name, apiKeyPrefix, active, createdAt, updatedAt
- `ProviderCreatedResponse` — above + apiKey (shown once only)
- `PaymentSummaryResponse` — totalAmount, totalCount, byProvider (List<ProviderBreakdownResponse>)
- `ProviderReconciliationResponse` — providerCode, providerName, totalAmount, totalCount, payments

### Error Response Structure
```json
{
  "timestamp": "2026-02-22T15:26:12Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields",
  "path": "/api/v1/customers",
  "validationErrors": {
    "firstName": "First name is required"
  }
}
```

---

## Running the Project

### Prerequisites
- Docker installed and running
- Java 21
- Maven

### 1. Start PostgreSQL
```bash
docker run --name local-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:16-alpine
```

If the container already exists:
```bash
docker start local-postgres
```

### 2. Build and Run

Quick start (skip tests):
```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

Full build with tests (requires Docker running for Testcontainers):
```bash
mvn clean verify
mvn spring-boot:run
```

API runs on `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

> Note: Docker Compose for local development is planned for Phase 7 (CI/CD).

---

## Test Coverage
163 passing tests covering:
- Customer CRUD with Luhn validation
- Payment processing with idempotency
- Provider lifecycle (onboard, deactivate, reactivate, regenerate key)
- Reporting queries with date range filters
- Integration tests use Testcontainers with PostgreSQL image — Docker must be running

---

## Completed Phases
- **Phase 1** — Project setup, Liquibase, PostgreSQL
- **Phase 2** — Customer onboarding, Luhn IDs
- **Phase 3** — Accounts + Payments, UUID v7 receipts
- **Phase 4** — Provider management, API key auth
- **Phase 5** — Reporting, reconciliation
- **Phase 6A** — Angular scaffold, shell layout
- **Phase 6B** — Customer list, detail, account payments
- **Phase 6C** — Dashboard, providers UI, dialogs

## Upcoming Phases

### Phase 6D — JWT Auth (3-4 days)
- Users table + Liquibase migration
- JWT service (access + refresh tokens)
- Auth filter chain
- Auth controller (login, refresh)
- SecurityConfig — dual filter chains (API key + JWT)
- Role-based access: ROLE_ADMIN, ROLE_OPERATOR
- Actuator security (health public, metrics protected)
- Angular: login page, auth guards, HTTP interceptor

### Phase 6E — Pagination + Sorting (2-3 days)
- Spring Data Pageable + Page<T> responses
- Customer list pagination and sorting
- Payment list pagination, sorting, and filtering (date range, provider, account)
- Provider list pagination
- Angular tables updated to handle paginated responses with page size and navigation

### Phase 7 — CI/CD
- Docker Compose for local development
- Jenkins pipeline
