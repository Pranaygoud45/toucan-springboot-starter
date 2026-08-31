package com.example.transactionstarter.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String id) {
        super("Transaction with ID '" + id + "' was not found.");
    }
}
