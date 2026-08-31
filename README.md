# Transaction Processing Service

A robust, production-ready RESTful Transaction Management Service built with **Java 17**, **Spring Boot 3.5.5**, **Spring Data JPA**, and **H2 Embedded Database** for the **Toucan Payments 2026 Fresher Engineering Trainee Programme**.

---

## 1. Problem Understanding & Core Architecture

The requirement is to implement a mini banking transaction processing service handling financial transactions. Each transaction is identified by a unique `transactionId` and associated with a `customerId`, `amount`, `currency`, `transactionType`, `status`, and creation timestamp.

### Design Principles:
- **Layered Architecture**: Clear separation of concerns between API Controllers, DTOs, Business Validation & Service logic, Exception Handling, and Data Access.
- **Precision with Currency**: Financial monetary amounts are strictly represented using `java.math.BigDecimal` to prevent floating-point precision loss.
- **Defensive Business Validation**: Strict validation rules for candidate variants before persisting state.
- **State Machine Integrity**: Deterministic transition matrix for transaction status life cycle to prevent illegal business state shifts.
- **Structured Error Standard**: Centralized REST error handling via `@RestControllerAdvice` producing standard HTTP status codes (`400`, `404`, `409`, `500`) with clear error objects.

---

## 2. Assigned Candidate Variant & Validation Rules

Per Section 10 of the Toucan Engineering Challenge specifications, this candidate submission is built against the following candidate variant rules:

- **Allowed Currencies**: `USD`, `EUR`, `GBP`, `INR` (Case-insensitive input, standardized to uppercase).
- **Maximum Amount Limit**: `10,000.00` per single transaction.
- **Allowed Transaction Types**: `PURCHASE`, `REFUND`, `TRANSFER`, `WITHDRAWAL`.
- **Initial Status**: Automatically assigned to `PENDING` upon creation.
- **Amount Validation**: Must be strictly positive ($> 0.00$) and not exceed `10,000.00`.

---

## 3. Status Transition Matrix & Rationale

Financial transactions must follow a strictly controlled status life cycle:

| Current Status | Allowed Target Statuses | Rationale |
| :--- | :--- | :--- |
| `PENDING` | `COMPLETED`, `FAILED`, `CANCELLED` | A pending transaction can succeed, be rejected by processing rules, or be cancelled by the user. |
| `COMPLETED` | `REFUNDED` | A settled transaction can only be refunded. It cannot be reverted back to pending or cancelled. |
| `FAILED` | *None (Terminal State)* | Failed processing attempts are permanent for auditability. |
| `CANCELLED` | *None (Terminal State)* | Cancelled orders cannot be reactivated. |
| `REFUNDED` | *None (Terminal State)* | Refunded transactions cannot be modified again. |

Any illegal transition attempt (e.g. `COMPLETED` $\rightarrow$ `PENDING`) triggers an `InvalidStatusTransitionException` and returns HTTP `400 Bad Request`.

---

## 4. API Endpoint Reference

### 1. Create Transaction
- **Method / Path**: `POST /api/transactions`
- **Request Body**:
```json
{
  "transactionId": "TXN-1001",
  "customerId": "CUST-500",
  "amount": 250.50,
  "currency": "USD",
  "transactionType": "PURCHASE"
}
```
- **Response**: `201 Created`
- **Error Statuses**: `400 Bad Request` (validation error), `409 Conflict` (duplicate `transactionId`).

---

### 2. Get Transaction by ID
- **Method / Path**: `GET /api/transactions/{id}`
- **Response**: `200 OK`
- **Error Statuses**: `404 Not Found`.

---

### 3. Update Transaction Status
- **Method / Path**: `PATCH /api/transactions/{id}/status`
- **Request Body**:
```json
{
  "status": "COMPLETED"
}
```
- **Response**: `200 OK`
- **Error Statuses**: `400 Bad Request` (invalid status transition), `404 Not Found`.

---

### 4. Get Customer Transactions
- **Method / Path**: `GET /api/transactions/customer/{customerId}`
- **Response**: `200 OK` (JSON array of transactions for specified customer).

---

## 5. Testing Strategy

Automated integration testing is performed using `@SpringBootTest` and `MockMvc` in `TransactionControllerTest.java`.

### Scenarios Covered:
1. **Successful Creation (`201 Created`)**: Validates complete flow and `PENDING` initial status.
2. **Invalid Input Rejection (`400 Bad Request`)**: Validates amount limits ($>10,000$) and currency validation (`XYZ`).
3. **Duplicate ID Rejection (`409 Conflict`)**: Verifies idempotent creation check.
4. **Non-existent Transaction (`404 Not Found`)**: Checks graceful handling of unknown IDs.
5. **Status Transition Validation**: Checks valid transition (`PENDING` $\rightarrow$ `COMPLETED`) and rejection of invalid transition (`COMPLETED` $\rightarrow$ `PENDING`).
6. **Customer Transactions Listing (`200 OK`)**: Verifies database query mapping for multiple records.

---

## 6. How to Run

Zero setup required. Run standard Maven build and test command:

```bash
./mvnw clean test
```

---

## 7. Known Limitations & Future Improvements

### Limitations:
- **In-Memory Storage**: Uses H2 embedded database; state resets upon application restart.
- **Single-Node Locking**: Concurrent transaction status updates rely on Spring transaction management without explicit optimistic locking (`@Version`).

### Future Improvements with More Time:
- **Optimistic Locking**: Add `@Version` field to `Transaction` entity to handle concurrent HTTP PATCH calls safely.
- **Idempotency Key Header**: Support `X-Idempotency-Key` headers for payment gateway safety.
- **Audit Logs**: Store full status transition history in an `audit_logs` table for compliance.
