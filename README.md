# Utility Account API — Spring Boot Backend

## Overview
Production-grade REST API for utility account management with mobile money payment integration (M-Pesa, MTN Mobile Money). Built as a portfolio project to demonstrate fintech backend architecture.

**Author:** Oualid Gharach  
**Target role:** Engineering Lead / Tech Lead  
**Live demo:** https://utility.oualidg.dev  
**Status:** Phase 7 complete

---

## Tech Stack
- Java 21
- Spring Boot 3.5
- Spring Security (JWT + cookie-based auth, API key auth)
- Spring Data JPA + Hibernate
- PostgreSQL 16
- Liquibase (database migrations)
- Lombok
- MapStruct
- SpringDoc OpenAPI (Swagger UI)
- Testcontainers (PostgreSQL image for integration tests)
- Maven
- Docker + Docker Compose
- GitHub Actions (CI/CD, self-hosted runner)
- Nginx (reverse proxy + static file serving)
- Cloudflare Tunnel (HTTPS, no port forwarding)

---

## Architecture Decisions
- **Luhn algorithm** for customer IDs (8-digit) and account numbers (10-digit)
- **UUID v7** for payment receipt numbers (time-sortable)
- **Soft delete** for providers (deactivate/reactivate pattern)
- **API key authentication** for payment providers (SHA-256 hashed, prefix stored, cache-aside pattern)
- **JWT + cookie-based auth** for admin/operator users (access + refresh tokens, HttpOnly cookies)
- **CSRF protection** via `CookieCsrfTokenRepository.withHttpOnlyFalse()` for Angular SPA compatibility
- **Role-based access control** — `ROLE_ADMIN` and `ROLE_OPERATOR` with `@PreAuthorize`
- **Dual filter chains** — API key chain for `/api/v1/*/payments`, JWT chain for everything else
- **Service layer pattern** with clear separation of concerns
- **Global exception handler** returning structured `validationErrors` map
- **Correlation ID filter** for request tracing
- **Pagination** on all list and search endpoints via Spring Data `Pageable`

---

## Database Schema
PostgreSQL database: `utility_account`

### Tables
- `customers` — customer profiles with Luhn-based IDs
- `accounts` — utility accounts (main + secondary) linked to customers
- `payment_providers` — external payment providers with hashed API keys
- `payments` — payment records with UUID v7 receipt numbers
- `users` — admin and operator users with BCrypt-hashed passwords

---

## API Endpoints

### Auth — `/api/auth`
| Method | Path | Description |
|--------|------|-------------|
| POST | `/login` | Login (cookie or bearer mode via `X-Auth-Mode` header) |
| POST | `/logout` | Logout and clear cookies |
| GET | `/me` | Get current session user |
| POST | `/refresh` | Refresh access token |

### Customers — `/api/v1/customers`
| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Onboard customer |
| GET | `/?page=&size=` | List all customers (paginated) |
| GET | `/{id}` | Get customer by ID |
| GET | `/search?mobile=` | Search by mobile number (paginated) |
| GET | `/search?surname=` | Search by surname (paginated) |
| GET | `/{id}/accounts` | Get customer accounts |
| PUT | `/{id}` | Update customer |
| DELETE | `/{id}` | Delete customer |

### Payments — `/api/v1`
| Method | Path | Description |
|--------|------|-------------|
| POST | `/customers/{id}/payments` | Make payment to main account |
| POST | `/accounts/{accountNumber}/payments` | Make payment to specific account |

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
| GET | `/payments?providerCode=&accountNumber=&receiptNumber=&from=&to=&page=&size=` | Search payments (paginated) |
| GET | `/accounts/{accountNumber}/summary` | Account lifetime totals |
| GET | `/providers/{providerCode}/summary?from=&to=` | Provider totals (lightweight) |
| GET | `/providers/{providerCode}/reconciliation?from=&to=` | Full provider settlement report (unbounded, CSV export) |

### Users — `/api/v1/users`
| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | List all users (admin only) |
| POST | `/` | Create user (admin only) |
| PATCH | `/{id}/password` | Change own password (operator) |
| PUT | `/{id}/password` | Reset any user password (admin only) |

---

## Error Response Structure
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

## Running Locally

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

Full build with tests (requires Docker for Testcontainers):
```bash
mvn clean verify
mvn spring-boot:run
```

API: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Test Coverage
261 passing tests covering:
- Customer CRUD with Luhn validation
- Payment processing with idempotency and atomic balance updates
- Transaction rollback scenarios
- Provider lifecycle (onboard, deactivate, reactivate, regenerate key)
- Reporting queries with pagination and date range filters
- JWT auth flows (login, logout, refresh, session restore)
- Role-based access control
- CSRF protection
- Integration tests use Testcontainers with real PostgreSQL — Docker must be running

## Load Test Results (k6, local)
- **196 payments/second** sustained throughput
- **p95 latency: 84ms**
- **0% error rate**
- Tested with 100 concurrent virtual users across MPESA and MTN providers

---

## Completed Phases
- **Phase 1** — Project setup, Liquibase, PostgreSQL
- **Phase 2** — Customer onboarding, Luhn IDs
- **Phase 3** — Accounts + Payments, UUID v7 receipts, atomic balance updates
- **Phase 4** — Provider management, API key auth (SHA-256, cache-aside)
- **Phase 5** — Reporting and reconciliation endpoints
- **Phase 6A–6B** — Provider management endpoints, payment dashboard
- **Phase 6C** — Angular frontend integration, CORS
- **Phase 6D** — JWT + cookie auth, RBAC, CSRF, user management, login UI
- **Phase 6E** — Pagination on all list/search endpoints, Angular paginator
- **Phase 7** — CI/CD pipeline, Docker Compose, Cloudflare Tunnel, live deployment

## Infrastructure
- **Server:** Ubuntu 24, 16GB RAM (home server)
- **CI/CD:** Two GitHub Actions pipelines (backend + frontend) with self-hosted runner — build, test, dockerize, push to GHCR, deploy via Docker Compose on every push to `main`
- **Containers:** Spring Boot + Nginx via Docker Compose; PostgreSQL runs standalone
- **Nginx:** Serves Angular static files, proxies `/api/*` to Spring Boot
- **Cloudflare Tunnel:** `utility.oualidg.dev` publicly accessible with HTTPS — no port forwarding or static IP required
- **Auth:** JWT cookies with `Secure: true` enforced via Cloudflare HTTPS

