# AI Usage Disclosure

Per Section 7 of the **Toucan Payments Engineering Challenge 2026** guidelines, this document outlines the utilization of AI tools during the development of this transaction management service.

---

## 1. Tools Used
- **Antigravity AI Agent / Claude 3.5 Sonnet & Gemini 3.6 Flash**: Used for project architectural planning, Java boilerplate drafting, and test setup.

---

## 2. Purpose & Tasks AI Was Used For
- **Scaffolding REST Endpoints**: Assistance in creating clean DTO and Controller signatures adhering to REST standards.
- **State Machine Rules Modeling**: Designing the status transition matrix logic (`isValidTransition`) for financial status flows.
- **MockMvc Test Patterns**: Formatting Spring `MockMvc` integration tests for HTTP assertions (`jsonPath`, `status()`).

---

## 3. Key AI Suggestions Changed, Corrected, or Rejected
- **Field Data Types**: Initial AI output suggested using standard `double` for monetary transaction amounts. This was **rejected and corrected to `BigDecimal`** to ensure financial accuracy and avoid floating-point rounding errors.
- **Exception Handling**: Default AI scaffolding relied on returning generic `400 Bad Request` strings without path context. This was **refactored into a custom `ErrorResponse` DTO** with explicit `timestamp`, `status`, `error`, `message`, and `path` attributes inside `@RestControllerAdvice`.
- **Status Updates**: AI generated a generic map parameter (`Map<String, String> body`) for status updates. This was **replaced with a strongly-typed `StatusUpdateRequest` DTO** with `@NotBlank` validation for standard schema contract enforcement.

---

## 4. How Verification Was Conducted
- Executed `./mvnw clean test` locally after every feature iteration to verify full compilation and test suite execution.
- Verified all 7 integration tests pass with zero errors, covering all required assessment scenarios.
