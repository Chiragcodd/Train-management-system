package com.chirag.train_management_system.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(BookingValidationException.class)
    public ResponseEntity<ErrorResponse> handleBookingValidation(
            BookingValidationException ex, HttpServletRequest request) {
        log.warn("Booking validation: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "Validation Error", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidRouteException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRoute(
            InvalidRouteException ex, HttpServletRequest request) {
        log.warn("Invalid route: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "Invalid Route", ex.getMessage(), request);
    }

    @ExceptionHandler(CustomAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            CustomAccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return build(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
    }

    @ExceptionHandler(SeatNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleSeatNotAvailable(
            SeatNotAvailableException ex, HttpServletRequest request) {
        log.warn("Seat not available: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return build(HttpStatus.CONFLICT, "Seat Not Available", ex.getMessage(), request);
    }

    @ExceptionHandler(SeatAlreadyBookedException.class)
    public ResponseEntity<ErrorResponse> handleSeatAlreadyBooked(
            SeatAlreadyBookedException ex, HttpServletRequest request) {
        log.warn("Seat booked: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return build(HttpStatus.CONFLICT, "Seat Booked", ex.getMessage(), request);
    }

    @ExceptionHandler(BookingCancellationException.class)
    public ResponseEntity<ErrorResponse> handleBookingCancellation(
            BookingCancellationException ex, HttpServletRequest request) {
        log.warn("Cancellation failed: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "Cancellation Failed", ex.getMessage(), request);
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(
            PaymentException ex, HttpServletRequest request) {
        log.warn("Payment error: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "Payment Error", ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad credentials | Path: {}", request.getRequestURI());
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized",
                "Invalid username or password.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

            Class<?> requiredType = ex.getRequiredType();

            String message = "Invalid value '" + ex.getValue()
                + "' for parameter '" + ex.getName() + "'.";

            if (requiredType != null && requiredType.isEnum()) {
            message += " Allowed values: "
                + java.util.Arrays.toString(requiredType.getEnumConstants());
            }

        log.warn("Type mismatch: {} | Path: {}", message, request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "Invalid Parameter", message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed JSON | Path: {}", request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "Malformed Request",
                "Request body is missing or has invalid JSON format.", request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Concurrent update conflict | Path: {}", request.getRequestURI());
        return build(HttpStatus.CONFLICT, "Concurrent Update",
                "Someone else updated this data simultaneously. Please retry your request.",
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation | Path: {} | Cause: {}",
                request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Data Conflict",
                "This operation conflicts with existing data. It may have already been processed.",
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Validation failed | Path: {} | Errors: {}",
                request.getRequestURI(), errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Validation Failed")
                        .message("Please check the request fields.")
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .validationErrors(errors)
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at path: {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Something went wrong. Please try again later.", request);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String error,
            String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .status(status.value())
                        .error(error)
                        .message(message)
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}