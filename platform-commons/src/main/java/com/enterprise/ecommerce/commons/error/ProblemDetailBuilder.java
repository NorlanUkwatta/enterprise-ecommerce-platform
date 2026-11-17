package com.enterprise.ecommerce.commons.error;

import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * GoF Builder Pattern for constructing standardized RFC 7807 / RFC 9457 {@link ProblemDetail}
 * error responses across all microservices.
 *
 * <p>Ensures every error inspected in Postman or consumed by API clients contains a uniform structure:
 * {@code type}, {@code title}, {@code status}, {@code detail}, {@code instance}, {@code errorCode},
 * {@code timestamp}, {@code correlationId}, and optional field-level {@code violations}.
 */
public final class ProblemDetailBuilder {

    private static final String PROBLEM_BASE_URI = "https://api.enterprise-ecommerce.com/problems/";

    private final ErrorCode errorCode;
    private String detail;
    private String instancePath;
    private String correlationId;
    private Instant timestamp = Instant.now();
    private final Map<String, String> fieldViolations = new LinkedHashMap<>();

    private ProblemDetailBuilder(ErrorCode errorCode) {
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.detail = errorCode.getDefaultTitle();
    }

    public static ProblemDetailBuilder forErrorCode(ErrorCode errorCode) {
        return new ProblemDetailBuilder(errorCode);
    }

    public static ProblemDetailBuilder fromException(BusinessException exception) {
        Objects.requireNonNull(exception, "exception must not be null");
        return new ProblemDetailBuilder(exception.getErrorCode())
                .withDetail(exception.getMessage());
    }

    public ProblemDetailBuilder withDetail(String detail) {
        if (detail != null && !detail.isBlank()) {
            this.detail = detail;
        }
        return this;
    }

    public ProblemDetailBuilder withInstancePath(String instancePath) {
        this.instancePath = instancePath;
        return this;
    }

    public ProblemDetailBuilder withCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    public ProblemDetailBuilder withTimestamp(Instant timestamp) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        return this;
    }

    public ProblemDetailBuilder addFieldViolation(String field, String message) {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(message, "message must not be null");
        this.fieldViolations.put(field, message);
        return this;
    }

    public ProblemDetail build() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.getHttpStatus(), detail);
        problem.setTitle(errorCode.getDefaultTitle());
        problem.setType(URI.create(PROBLEM_BASE_URI + errorCode.name().toLowerCase().replace('_', '-')));

        if (instancePath != null && !instancePath.isBlank()) {
            problem.setInstance(URI.create(instancePath));
        }

        problem.setProperty("errorCode", errorCode.getCode());
        problem.setProperty("timestamp", timestamp.toString());

        if (correlationId != null && !correlationId.isBlank()) {
            problem.setProperty("correlationId", correlationId);
        }

        if (!fieldViolations.isEmpty()) {
            problem.setProperty("violations", Map.copyOf(fieldViolations));
        }

        return problem;
    }
}
