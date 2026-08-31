package com.example.transactionstarter.exception;

public class DuplicateTransactionException extends RuntimeException {
    public DuplicateTransactionException(String id) {
        super("Transaction with ID '" + id + "' already exists.");
    }
}
