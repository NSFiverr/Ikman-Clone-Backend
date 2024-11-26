//package com.marketplace.platform.exception;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(InvalidCredentialsException.class)
//    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(InvalidCredentialsException ex) {
//        ErrorResponse error = new ErrorResponse(
//                HttpStatus.UNAUTHORIZED.value(),
//                "Authentication failed",
//                ex.getMessage()
//        );
//        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
//    }
//}