package com.fram.insurance_manager.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String TIMESTAMP = "timestamp";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    private static final String ERROR = "error";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        StringBuilder errorMessages = new StringBuilder();

        ex.getBindingResult().getFieldErrors().forEach(e ->
                errorMessages
                        .append(e.getDefaultMessage())
                        .append(". ")
        );

        Map<String, Object> response = new HashMap<>();
        response.put(TIMESTAMP, LocalDateTime.now());
        response.put(STATUS, HttpStatus.PRECONDITION_FAILED.value());
        response.put(MESSAGE, errorMessages.toString().trim());
        response.put(ERROR, HttpStatus.PRECONDITION_FAILED.getReasonPhrase());

        return new ResponseEntity<>(response, HttpStatus.PRECONDITION_FAILED);
    }


    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(Map.of(
                        TIMESTAMP, LocalDateTime.now(),
                        STATUS, ex.getStatusCode().value(),
                        ERROR, ex.getStatusCode(),
                        MESSAGE, Objects.requireNonNull(ex.getReason())
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        ERROR, HttpStatus.UNAUTHORIZED.value(),
                        STATUS, HttpStatus.UNAUTHORIZED,
                        TIMESTAMP, LocalDateTime.now(),
                        MESSAGE, (ex.getMessage().equals("Bad credentials") ? "Credenciales inválidas" : ex.getMessage())
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        TIMESTAMP, LocalDateTime.now(),
                        STATUS, HttpStatus.FORBIDDEN.value(),
                        ERROR, HttpStatus.FORBIDDEN,
                        MESSAGE, "No tienes permiso para acceder a este recurso"
                ));
    }
}
