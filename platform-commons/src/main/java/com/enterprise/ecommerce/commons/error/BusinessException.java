package com.enterprise.ecommerce.commons.error;

import java.util.Objects;

/**
 * Base runtime exception for domain rule violations across microservices.
 * Maps directly to an {@link ErrorCode} and RFC 7807 ProblemDetail payload.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(Objects.requireNonNull(detailMessage, "detailMessage must not be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public BusinessException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(Objects.requireNonNull(detailMessage, "detailMessage must not be null"), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
