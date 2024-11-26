package com.marketplace.platform.exception;

import com.marketplace.platform.dto.response.AuthErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class AuthenticationExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<AuthErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {
        return createErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(AccountDeactivatedException.class)
    public ResponseEntity<AuthErrorResponse> handleAccountDeactivated(
            AccountDeactivatedException ex,
            HttpServletRequest request) {
        return createErrorResponse(
                HttpStatus.FORBIDDEN,
                "Account deactivated",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    private ResponseEntity<AuthErrorResponse> createErrorResponse(
            HttpStatus status,
            String error,
            String message,
            String path,
            Long retryAfter) {

        AuthErrorResponse response = AuthErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .retryAfterMillis(retryAfter)
                .build();

        return new ResponseEntity<>(response, status);
    }
}