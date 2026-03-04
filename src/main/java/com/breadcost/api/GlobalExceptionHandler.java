package com.breadcost.api;

import com.breadcost.commands.CommandResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for REST API
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommandResult> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(CommandResult.builder()
                        .success(false)
                        .errorCode("ERR_VALIDATION")
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommandResult> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        
        log.warn("Validation error: {}", message);
        return ResponseEntity
                .badRequest()
                .body(CommandResult.builder()
                        .success(false)
                        .errorCode("ERR_VALIDATION")
                        .message(message)
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CommandResult> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(CommandResult.builder()
                        .success(false)
                        .errorCode("ERR_FORBIDDEN")
                        .message("Access denied")
                        .build());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<CommandResult> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(CommandResult.builder()
                        .success(false)
                        .errorCode("ERR_INVALID_STATE")
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CommandResult> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(CommandResult.builder()
                        .success(false)
                        .errorCode("ERR_CONFLICT")
                        .message("A record with this identifier already exists")
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommandResult> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommandResult.builder()
                        .success(false)
                        .errorCode("ERR_INTERNAL")
                        .message("An unexpected error occurred")
                        .build());
    }
}
