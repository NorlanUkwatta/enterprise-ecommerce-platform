package com.enterprise.ecommerce.commons.trace;

import com.enterprise.ecommerce.commons.id.UuidV7Generator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Servlet Filter that guarantees every incoming HTTP request has a valid {@code X-Correlation-Id}.
 *
 * <p><b>Why for Observability (ELK Stack) & Postman Testing:</b>
 * <ul>
 *   <li>If the caller (API Gateway or Postman) supplies a valid {@code X-Correlation-Id} header,
 *       it is preserved across downstream microservice calls.</li>
 *   <li>If absent or malformed, a new time-ordered {@link UuidV7Generator} ID is generated.</li>
 *   <li>Binds {@code correlationId} to SLF4J {@link MDC} so every Logstash JSON log line in
 *       Elasticsearch/Kibana can be filtered by a single request ID.</li>
 *   <li>Echoes {@code X-Correlation-Id} in the HTTP response header for immediate inspection in Postman.</li>
 * </ul>
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";

    /**
     * Strict allowlist pattern (alphanumeric, hyphens, underscores, 8..64 chars)
     * to prevent HTTP Response Splitting / CRLF Log Injection attacks.
     */
    private static final Pattern SAFE_CORRELATION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_]{8,64}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String incomingCorrelationId = request.getHeader(CORRELATION_ID_HEADER);
        String correlationId = isValidCorrelationId(incomingCorrelationId)
                ? incomingCorrelationId
                : UuidV7Generator.generate().toString();

        MDC.put(MDC_CORRELATION_ID_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION_ID_KEY);
        }
    }

    private static boolean isValidCorrelationId(String candidate) {
        return candidate != null && SAFE_CORRELATION_ID_PATTERN.matcher(candidate).matches();
    }
}
