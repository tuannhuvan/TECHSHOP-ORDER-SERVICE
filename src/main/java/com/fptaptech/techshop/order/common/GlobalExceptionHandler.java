package com.fptaptech.techshop.order.common;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String ERROR_BASE = "https://api.techshop.example.com/errors/";

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleOrderNotFound(OrderNotFoundException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(ERROR_BASE + "order-not-found"));
        pd.setTitle("Order not found");
        pd.setProperty("code", "ORDER_NOT_FOUND");
        pd.setProperty("orderCode", ex.getOrderCode());
        pd.setProperty("timestamp", Instant.now());
        setInstance(pd, request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<ProblemDetail> handleInvalidTransition(InvalidTransitionException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create(ERROR_BASE + "invalid-transition"));
        pd.setTitle("Invalid order state transition");
        pd.setProperty("code", "ORDER_INVALID_TRANSITION");
        pd.setProperty("currentStatus", ex.getCurrentStatus());
        pd.setProperty("action", ex.getAction());
        pd.setProperty("timestamp", Instant.now());
        setInstance(pd, request);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ProblemDetail> handleIdempotencyConflict(IdempotencyConflictException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create(ERROR_BASE + "idempotency-conflict"));
        pd.setTitle("Idempotency conflict");
        pd.setProperty("code", "IDEMPOTENCY_CONFLICT");
        pd.setProperty("idempotencyKey", ex.getIdempotencyKey());
        pd.setProperty("timestamp", Instant.now());
        setInstance(pd, request);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(PreconditionFailedException.class)
    public ResponseEntity<ProblemDetail> handlePreconditionFailed(PreconditionFailedException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_FAILED, ex.getMessage());
        pd.setType(URI.create(ERROR_BASE + "precondition-failed"));
        pd.setTitle("Precondition failed");
        pd.setProperty("code", "PRECONDITION_FAILED");
        pd.setProperty("expectedETag", ex.getExpectedETag());
        pd.setProperty("actualETag", ex.getActualETag());
        pd.setProperty("timestamp", Instant.now());
        setInstance(pd, request);
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(pd);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        pd.setType(URI.create(ERROR_BASE + "unauthorized"));
        pd.setTitle("Unauthorized");
        pd.setProperty("code", "UNAUTHORIZED");
        pd.setProperty("timestamp", Instant.now());
        setInstance(pd, request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ProblemDetail> handleForbidden(ForbiddenException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setType(URI.create(ERROR_BASE + "forbidden"));
        pd.setTitle("Forbidden");
        pd.setProperty("code", "FORBIDDEN");
        pd.setProperty("timestamp", Instant.now());
        setInstance(pd, request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(BadRequestException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create(ERROR_BASE + "validation"));
        pd.setTitle("Invalid input");
        pd.setProperty("code", "INVALID_INPUT");
        pd.setProperty("timestamp", Instant.now());
        setInstance(pd, request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = ex.getBody();
        pd.setType(URI.create(ERROR_BASE + "file-too-large"));
        pd.setTitle("File too large");
        pd.setDetail("Kích thước tệp vượt quá giới hạn cho phép (tối đa 5MB)");
        pd.setProperty("code", "FILE_TOO_LARGE");
        pd.setProperty("timestamp", Instant.now());
        setInstance(pd, request);
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = ex.getBody();
        pd.setType(URI.create(ERROR_BASE + "validation"));
        pd.setTitle("Validation failed");
        pd.setProperty("code", "INVALID_INPUT");
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("errors",
                ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> Map.of("field", fe.getField(),
                                "message", String.valueOf(fe.getDefaultMessage())))
                        .toList());
        setInstance(pd, request);
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    private void setInstance(ProblemDetail pd, WebRequest request) {
        if (request instanceof ServletWebRequest swr) {
            pd.setInstance(URI.create(swr.getRequest().getRequestURI()));
        }
    }
}
