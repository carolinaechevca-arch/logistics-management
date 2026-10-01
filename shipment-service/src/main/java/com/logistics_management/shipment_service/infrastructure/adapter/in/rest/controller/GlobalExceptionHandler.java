package com.logistics_management.shipment_service.infrastructure.adapter.in.rest.controller;

import com.logistics_management.shipment_service.domain.exception.DuplicateTrackingNumberException;
import com.logistics_management.shipment_service.domain.exception.IdempotencyKeyConflictException;
import com.logistics_management.shipment_service.domain.exception.InvalidIdempotencyKeyException;
import com.logistics_management.shipment_service.domain.exception.InvalidShipmentStatusTransitionException;
import com.logistics_management.shipment_service.domain.exception.ShipmentCannotBeCancelledException;
import com.logistics_management.shipment_service.domain.exception.ShipmentNotFoundException;
import com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(ShipmentNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(RuntimeException exception, HttpServletRequest request) {
        log.warn("shipment request not found path={} message={}", request.getRequestURI(), exception.getMessage());
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({InvalidShipmentStatusTransitionException.class, ShipmentCannotBeCancelledException.class,
            DuplicateTrackingNumberException.class, IdempotencyKeyConflictException.class})
    ResponseEntity<ApiErrorResponse> conflict(RuntimeException exception, HttpServletRequest request) {
        log.warn("shipment business conflict path={} message={}", request.getRequestURI(), exception.getMessage());
        return response(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MissingRequestHeaderException.class,
            IllegalArgumentException.class, InvalidIdempotencyKeyException.class})
    ResponseEntity<ApiErrorResponse> badRequest(Exception exception, HttpServletRequest request) {
        log.warn("invalid shipment request path={} message={}", request.getRequestURI(), validationMessage(exception));
        return response(HttpStatus.BAD_REQUEST, validationMessage(exception), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> unexpected(Exception exception, HttpServletRequest request) {
        log.error("unhandled request error path={}", request.getRequestURI(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected internal error", request);
    }

    private String validationMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException validationException) {
            return validationException.getBindingResult().getFieldErrors().stream()
                    .findFirst().map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .orElse("Invalid request");
        }
        return exception.getMessage() == null ? "Invalid request" : exception.getMessage();
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(LocalDateTime.now(), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
