package com.example.transactionstarter.transaction;

import com.example.transactionstarter.exception.DuplicateTransactionException;
import com.example.transactionstarter.exception.InvalidStatusTransitionException;
import com.example.transactionstarter.exception.InvalidTransactionException;
import com.example.transactionstarter.exception.TransactionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class TransactionService {

    private final TransactionRepository repository;

    private static final Set<String> ALLOWED_CURRENCIES = Set.of("USD", "EUR", "GBP", "INR");
    private static final Set<String> ALLOWED_TRANSACTION_TYPES = Set.of("PURCHASE", "REFUND", "TRANSFER", "WITHDRAWAL");
    private static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("10000.00");

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Transaction createTransaction(TransactionRequest request) {
        if (repository.existsById(request.getTransactionId())) {
            throw new DuplicateTransactionException(request.getTransactionId());
        }

        validateBusinessRules(request);

        Transaction transaction = new Transaction(
                request.getTransactionId(),
                request.getCustomerId(),
                request.getAmount(),
                request.getCurrency().toUpperCase(),
                request.getTransactionType().toUpperCase(),
                "PENDING"
        );

        return repository.save(transaction);
    }

    @Transactional(readOnly = true)
    public Transaction getTransaction(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Transactional
    public Transaction updateTransactionStatus(String id, String newStatusRaw) {
        if (newStatusRaw == null || newStatusRaw.trim().isEmpty()) {
            throw new InvalidTransactionException("Target status cannot be empty.");
        }

        Transaction transaction = getTransaction(id);
        String currentStatus = transaction.getStatus();
        String newStatus = newStatusRaw.trim().toUpperCase();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, newStatus);
        }

        transaction.setStatus(newStatus);
        return repository.save(transaction);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getCustomerTransactions(String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new InvalidTransactionException("CustomerId cannot be empty.");
        }
        return repository.findByCustomerId(customerId);
    }

    private void validateBusinessRules(TransactionRequest request) {
        String currency = request.getCurrency().toUpperCase();
        if (!ALLOWED_CURRENCIES.contains(currency)) {
            throw new InvalidTransactionException("Unsupported currency '" + request.getCurrency() + "'. Allowed currencies: " + ALLOWED_CURRENCIES);
        }

        String type = request.getTransactionType().toUpperCase();
        if (!ALLOWED_TRANSACTION_TYPES.contains(type)) {
            throw new InvalidTransactionException("Unsupported transaction type '" + request.getTransactionType() + "'. Allowed types: " + ALLOWED_TRANSACTION_TYPES);
        }

        if (request.getAmount().compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new InvalidTransactionException("Transaction amount " + request.getAmount() + " exceeds maximum threshold of " + MAX_TRANSACTION_AMOUNT);
        }
    }

    private boolean isValidTransition(String currentStatus, String targetStatus) {
        if (currentStatus.equalsIgnoreCase(targetStatus)) {
            return true;
        }

        return switch (currentStatus.toUpperCase()) {
            case "PENDING" -> Set.of("COMPLETED", "FAILED", "CANCELLED").contains(targetStatus);
            case "COMPLETED" -> Set.of("REFUNDED").contains(targetStatus);
            case "FAILED", "CANCELLED", "REFUNDED" -> false;
            default -> false;
        };
    }
}
