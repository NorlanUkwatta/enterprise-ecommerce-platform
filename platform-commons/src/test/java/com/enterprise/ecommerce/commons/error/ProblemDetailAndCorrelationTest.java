package com.enterprise.ecommerce.commons.error;

import com.enterprise.ecommerce.commons.trace.CorrelationIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailAndCorrelationTest {

    @Test
    @DisplayName("Should build standardized RFC 7807 ProblemDetail from BusinessException with field violations")
    void shouldBuildStandardizedProblemDetail() {
        Instant fixedTime = Instant.parse("2026-11-17T08:00:00Z");
        BusinessException exception = new BusinessException(
                ErrorCode.COUPON_QUOTA_EXCEEDED,
                "Coupon BLACKFRIDAY50 has reached its maximum 10,000 redemptions");

        ProblemDetail problem = ProblemDetailBuilder.fromException(exception)
                .withInstancePath("/api/v1/coupons/redeem")
                .withCorrelationId("corr-12345678")
                .withTimestamp(fixedTime)
                .addFieldViolation("couponCode", "Redemption limit reached")
                .build();

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getTitle()).isEqualTo("Coupon Redemption Quota Exceeded");
        assertThat(problem.getDetail()).isEqualTo("Coupon BLACKFRIDAY50 has reached its maximum 10,000 redemptions");
        assertThat(problem.getType().toString())
                .isEqualTo("https://api.enterprise-ecommerce.com/problems/coupon-quota-exceeded");
        assertThat(problem.getInstance().toString()).isEqualTo("/api/v1/coupons/redeem");
        assertThat(problem.getProperties())
                .containsEntry("errorCode", "ERR-422-002")
                .containsEntry("correlationId", "corr-12345678")
                .containsEntry("timestamp", "2026-11-17T08:00:00Z")
                .containsEntry("violations", Map.of("couponCode", "Redemption limit reached"));
    }

    @Test
    @DisplayName("Should preserve valid incoming X-Correlation-Id header and clear MDC after request completes")
    void shouldPreserveValidIncomingCorrelationId() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "postman-req-987654321");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcInsideFilter = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) ->
                mdcInsideFilter.set(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)));

        assertThat(mdcInsideFilter.get()).isEqualTo("postman-req-987654321");
        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("postman-req-987654321");
        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("Should generate UUIDv7 correlation ID when header is missing or contains CRLF injection characters")
    void shouldGenerateUuidV7WhenCorrelationIdMissingOrMalicious() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "bad\r\nInjected-Header: hacked");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String generatedHeader = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generatedHeader).isNotNull();
        UUID parsed = UUID.fromString(generatedHeader);
        assertThat(parsed.version()).isEqualTo(7);
        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
    }
}
