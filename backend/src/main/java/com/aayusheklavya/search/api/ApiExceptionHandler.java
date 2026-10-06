package com.aayusheklavya.search.api;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ProblemDetail constraint(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request parameters");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException e) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail status(ResponseStatusException e) {
        return problem(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason());
    }

    /** Elasticsearch rejected the request (bad query, missing index...): our fault or the cluster's, not the caller's. */
    @ExceptionHandler(ElasticsearchException.class)
    public ProblemDetail elasticsearch(ElasticsearchException e) {
        log.error("Elasticsearch error: {}", e.response(), e);
        return problem(HttpStatus.BAD_GATEWAY, "Search backend rejected the request");
    }

    @ExceptionHandler(UncheckedIOException.class)
    public ProblemDetail unavailable(UncheckedIOException e) {
        log.error("Elasticsearch unreachable", e);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Search backend is unavailable");
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        return pd;
    }
}
