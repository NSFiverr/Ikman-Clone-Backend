package com.marketplace.platform.exception;

public class EnvironmentValidationException extends RuntimeException {
    public EnvironmentValidationException(String message) {
        super(message);
    }
}