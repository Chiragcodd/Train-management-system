package com.chirag.train_management_system.exception;

public class CustomAccessDeniedException extends RuntimeException {

    public CustomAccessDeniedException() {
        super("You do not have permission to perform this action.");
    }

    public CustomAccessDeniedException(String message) {
        super(message);
    }
}