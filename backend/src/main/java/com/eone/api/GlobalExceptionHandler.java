package com.eone.api;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<?> tooLarge(Exception ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("message", "Files must be 10 MB or smaller"));
    }

    @ExceptionHandler({org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class})
    ResponseEntity<?> missingPart(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Provide the required upload fields and file"));
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("timestamp", Instant.now(),
                "status", 409, "code", "DATA_CONFLICT", "message", "This change conflicts with existing data"));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> malformed(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("timestamp", Instant.now(),
                "status", 400, "code", "INVALID_REQUEST", "message", "Check the request values and try again"));
    }
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> api(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("timestamp", Instant.now(), "status", ex.getStatus().value(),
                "code", ex.getStatus().name(), "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(java.util.stream.Collectors.toMap(e -> e.getField(), e -> Optional.ofNullable(e.getDefaultMessage()).orElse("invalid"), (a,b)->a));
        return ResponseEntity.badRequest().body(Map.of("timestamp", Instant.now(), "message", "Validation failed", "errors", errors));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> generic(Exception ex) {
        // Do not log request bodies, tokens, SQL parameter values or exception messages.
        log.error("Unhandled API failure type={}", ex.getClass().getName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("timestamp", Instant.now(), "message", "Unexpected server error"));
    }
}
