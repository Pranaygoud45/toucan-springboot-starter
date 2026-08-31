package com.example.transactionstarter.exception;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String currentStatus, String targetStatus) {
        super("Invalid status transition from '" + currentStatus + "' to '" + targetStatus + "'.");
    }
}
