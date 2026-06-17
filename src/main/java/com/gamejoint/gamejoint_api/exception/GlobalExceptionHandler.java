package com.gamejoint.gamejoint_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// This annotation tells Spring to intercept all errors before they leave the server
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Catch 404 Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 2. Catch 403 Forbidden
    @ExceptionHandler(AccountRestrictedException.class)
    public ResponseEntity<Object> handleForbidden(AccountRestrictedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // 3. Catch 409 Conflicts (We can group multiple exceptions into one handler!)
    @ExceptionHandler({DuplicateResourceException.class, UserAlreadyExistsException.class})
    public ResponseEntity<Object> handleConflicts(RuntimeException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 4. Catch 401 Unauthorized
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Object> handleUnauthorized(InvalidCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // The standardized JSON Builder
    private ResponseEntity<Object> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        
        return new ResponseEntity<>(body, status);
    }
}