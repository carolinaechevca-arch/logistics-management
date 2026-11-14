package com.logistics_management.shipment_service.infrastructure.adapter.in.rest.controller;

import com.logistics_management.shipment_service.domain.exception.DuplicateTrackingNumberException;
import com.logistics_management.shipment_service.domain.exception.ShipmentNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/shipments/test");
    }

    @Test
    void mapsNotFoundConflictAndUnexpectedErrors() {
        var notFound = handler.notFound(new ShipmentNotFoundException(UUID.randomUUID()), request);
        var conflict = handler.conflict(new DuplicateTrackingNumberException("TRK-1"), request);
        var unexpected = handler.unexpected(new IllegalStateException("failure"), request);

        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(notFound.getBody().path()).isEqualTo("/api/v1/shipments/test");
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getBody().message()).contains("TRK-1");
        assertThat(unexpected.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(unexpected.getBody().message()).isEqualTo("Unexpected internal error");
    }

    @Test
    void mapsOrdinaryAndNullMessagesAsBadRequests() {
        var ordinary = handler.badRequest(new IllegalArgumentException("invalid value"), request);
        var nullMessage = handler.badRequest(new IllegalArgumentException((String) null), request);

        assertThat(ordinary.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ordinary.getBody().message()).isEqualTo("invalid value");
        assertThat(nullMessage.getBody().message()).isEqualTo("Invalid request");
    }

    @Test
    void extractsFirstValidationFieldErrorOrUsesFallback() {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Object(), "request");
        errors.addError(new FieldError("request", "customerEmail", "must be valid"));
        MethodArgumentNotValidException validation = new MethodArgumentNotValidException(null, errors);
        var withField = handler.badRequest(validation, request);

        BeanPropertyBindingResult emptyErrors = new BeanPropertyBindingResult(new Object(), "request");
        MethodArgumentNotValidException empty = new MethodArgumentNotValidException(null, emptyErrors);
        var withoutField = handler.badRequest(empty, request);

        assertThat(withField.getBody().message()).isEqualTo("customerEmail: must be valid");
        assertThat(withoutField.getBody().message()).isEqualTo("Invalid request");
    }
}
