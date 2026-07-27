package com.rakeshgupta.fastchat_backend.exception;

import com.rakeshgupta.fastchat_backend.common.exception.FileProcessingException;
import com.rakeshgupta.fastchat_backend.common.exception.UnsupportedFileTypeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, Object> body = baseError(HttpStatus.BAD_REQUEST, "Validation failed", req.getRequestURI());
        Map<String, String> fields = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.put(fe.getField(), fe.getDefaultMessage());
        }
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        Map<String, Object> body = baseError(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        Map<String, Object> body = baseError(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        log.warn("File upload exceeds maximum allowed size: {}", ex.getMessage());
        Map<String, Object> body = baseError(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File size exceeds the maximum allowed limit of 10 MB",
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<?> handleFileProcessing(FileProcessingException ex, HttpServletRequest req) {
        log.error("File processing error for '{}': {}", ex.getFilename(), ex.getMessage());
        Map<String, Object> body = baseError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error processing file: " + ex.getFilename(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<?> handleUnsupportedFileType(UnsupportedFileTypeException ex, HttpServletRequest req) {
        log.warn("Unsupported file type '{}' for '{}'", ex.getExtension(), ex.getFilename());
        Map<String, Object> body = baseError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        Map<String, Object> body = baseError(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Map<String, Object> baseError(HttpStatus status, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return body;
    }
}
