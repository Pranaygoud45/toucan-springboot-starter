package com.example.transactionstarter.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    @DisplayName("A. Create Transaction - Success (201 Created)")
    void testCreateTransaction_Success() throws Exception {
        TransactionRequest request = new TransactionRequest(
                "TXN-1001",
                "CUST-500",
                new BigDecimal("250.50"),
                "USD",
                "PURCHASE"
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId", is("TXN-1001")))
                .andExpect(jsonPath("$.customerId", is("CUST-500")))
                .andExpect(jsonPath("$.amount", is(250.50)))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.transactionType", is("PURCHASE")))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    @DisplayName("B. Create Transaction - Rejected due to Invalid Input / Business Validation (400 Bad Request)")
    void testCreateTransaction_ValidationFailure() throws Exception {
        TransactionRequest request = new TransactionRequest(
                "TXN-1002",
                "CUST-500",
                new BigDecimal("15000.00"),
                "XYZ",
                "PURCHASE"
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("C. Create Transaction - Rejected Duplicate Transaction ID (409 Conflict)")
    void testCreateTransaction_DuplicateId() throws Exception {
        TransactionRequest request = new TransactionRequest(
                "TXN-DUP-01",
                "CUST-500",
                new BigDecimal("100.00"),
                "INR",
                "PURCHASE"
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("Transaction with ID 'TXN-DUP-01' already exists.")));
    }

    @Test
    @DisplayName("D. Get Transaction - Non-existent ID (404 Not Found)")
    void testGetTransaction_NotFound() throws Exception {
        mockMvc.perform(get("/api/transactions/NON-EXISTENT-ID"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Transaction with ID 'NON-EXISTENT-ID' was not found.")));
    }

    @Test
    @DisplayName("E. Update Transaction Status - Valid & Invalid Transitions")
    void testUpdateTransactionStatus() throws Exception {
        TransactionRequest createReq = new TransactionRequest(
                "TXN-STATUS-01",
                "CUST-600",
                new BigDecimal("499.99"),
                "EUR",
                "TRANSFER"
        );
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        StatusUpdateRequest updateReq = new StatusUpdateRequest("COMPLETED");
        mockMvc.perform(patch("/api/transactions/TXN-STATUS-01/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        StatusUpdateRequest invalidReq = new StatusUpdateRequest("PENDING");
        mockMvc.perform(patch("/api/transactions/TXN-STATUS-01/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Invalid status transition from 'COMPLETED' to 'PENDING'.")));
    }

    @Test
    @DisplayName("F. Get Customer Transactions - List Retrieval (200 OK)")
    void testGetCustomerTransactions() throws Exception {
        Transaction t1 = new Transaction("TXN-CUST-1", "CUST-777", new BigDecimal("100.00"), "GBP", "WITHDRAWAL", "PENDING");
        Transaction t2 = new Transaction("TXN-CUST-2", "CUST-777", new BigDecimal("200.00"), "GBP", "PURCHASE", "COMPLETED");
        transactionRepository.save(t1);
        transactionRepository.save(t2);

        mockMvc.perform(get("/api/transactions/customer/CUST-777"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].transactionId", containsInAnyOrder("TXN-CUST-1", "TXN-CUST-2")));
    }
}
