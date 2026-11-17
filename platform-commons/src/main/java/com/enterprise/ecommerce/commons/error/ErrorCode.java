package com.enterprise.ecommerce.commons.error;

import org.springframework.http.HttpStatus;

/**
 * Standardized catalog of machine-readable domain error codes returned in RFC 7807 ProblemDetail responses.
 */
public enum ErrorCode {

    VALIDATION_FAILED("ERR-400-001", HttpStatus.BAD_REQUEST, "Request Validation Failed"),
    UNAUTHORIZED("ERR-401-001", HttpStatus.UNAUTHORIZED, "Authentication Required"),
    FORBIDDEN("ERR-403-001", HttpStatus.FORBIDDEN, "Access Denied"),
    RESOURCE_NOT_FOUND("ERR-404-001", HttpStatus.NOT_FOUND, "Resource Not Found"),
    IDEMPOTENCY_CONFLICT("ERR-409-001", HttpStatus.CONFLICT, "Duplicate or In-Flight Idempotent Request"),
    OPTIMISTIC_LOCK_CONFLICT("ERR-409-002", HttpStatus.CONFLICT, "Concurrent Modification Conflict"),
    COUPON_INVALID_OR_EXPIRED("ERR-422-001", HttpStatus.UNPROCESSABLE_ENTITY, "Coupon Invalid or Expired"),
    COUPON_QUOTA_EXCEEDED("ERR-422-002", HttpStatus.UNPROCESSABLE_ENTITY, "Coupon Redemption Quota Exceeded"),
    INSUFFICIENT_INVENTORY("ERR-422-003", HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient Warehouse Stock"),
    RATE_LIMIT_EXCEEDED("ERR-429-001", HttpStatus.TOO_MANY_REQUESTS, "Rate Limit Exceeded"),
    PAYMENT_GATEWAY_FAILURE("ERR-502-001", HttpStatus.BAD_GATEWAY, "Upstream Payment Gateway Failure"),
    INTERNAL_SERVER_ERROR("ERR-500-001", HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Internal Server Error");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultTitle;

    ErrorCode(String code, HttpStatus httpStatus, String defaultTitle) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultTitle = defaultTitle;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }
}
