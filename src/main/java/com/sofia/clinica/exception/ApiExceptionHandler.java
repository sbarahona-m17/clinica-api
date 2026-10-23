package com.sofia.clinica.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ApiError(Instant timestamp, int status, String message, String path,
                           Map<String, String> errors) {}

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(ConflictException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrity(HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "La operación entra en conflicto con los datos existentes.", request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new TreeMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.put(e.getField(), e.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Revisa los campos enviados.", request, fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> malformed(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST,
                "Formato inválido. Revisa el JSON, los identificadores, las fechas y el estado de la cita.", request, Map.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message,
                                            HttpServletRequest request, Map<String, String> errors) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), message,
                request.getRequestURI(), errors));
    }
}
