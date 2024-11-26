package com.marketplace.platform.exception;

/**
 * Exception thrown when a token is invalid (malformed, not found, or otherwise invalid)
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}