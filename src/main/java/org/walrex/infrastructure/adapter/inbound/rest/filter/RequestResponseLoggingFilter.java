package org.walrex.infrastructure.adapter.inbound.rest.filter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.MDC;

import java.time.Instant;

/**
 * Filtro para logging detallado de requests y responses HTTP.
 *
 * Captura:
 * - Headers de request
 * - Body de request (si existe)
 * - Query parameters
 * - Path parameters
 * - Response status
 * - Response body
 * - Tiempo de procesamiento
 */
@Slf4j
@ApplicationScoped
public class RequestResponseLoggingFilter {

    /**
     * Intercepta todas las requests y responses.
     * Priority 1 = se ejecuta antes que otros filtros.
     *
     * Este filtro maneja tanto el logging del request (inmediato) como del response
     * (mediante un endHandler que se ejecuta cuando la respuesta está lista).
     */
    @RouteFilter(1)
    void logRequestAndResponse(RoutingContext rc) {
        long startTime = System.currentTimeMillis();
        String requestId = generateRequestId();

        // Capturar trace ID y span ID de OpenTelemetry
        Span currentSpan = Span.current();
        SpanContext spanContext = currentSpan.getSpanContext();
        String traceId = spanContext.getTraceId();
        String spanId = spanContext.getSpanId();

        // Agregar trace IDs al MDC para que aparezcan en logs
        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);
        MDC.put("requestId", requestId);

        // Guardar en el context para usarlo después
        rc.put("requestId", requestId);
        rc.put("traceId", traceId);
        rc.put("spanId", spanId);
        rc.put("startTime", startTime);

        // Agregar trace IDs como response headers para facilitar debugging
        rc.response().putHeader("X-Trace-Id", traceId);
        rc.response().putHeader("X-Span-Id", spanId);
        rc.response().putHeader("X-Request-Id", requestId);

        // Log de información del request
        logIncomingRequest(rc, requestId, traceId, spanId);

        // Agregar listener para cuando la respuesta termine
        rc.addEndHandler(result -> {
            String reqId = rc.get("requestId");
            String trId = rc.get("traceId");
            Long start = rc.get("startTime");

            if (start != null) {
                long duration = System.currentTimeMillis() - start;
                logOutgoingResponse(rc, reqId, trId, duration);
            }

            // Limpiar MDC después de procesar la request
            MDC.clear();
        });

        // Continuar con el siguiente handler
        rc.next();
    }

    private void logIncomingRequest(RoutingContext rc, String requestId, String traceId, String spanId) {
        String method = rc.request().method().name();
        String uri = rc.request().uri();
        String path = rc.request().path();

        log.info("🔵 INCOMING REQUEST [reqId:{}] [trace:{}] {} {} - Path: {}",
            requestId, traceId, method, uri, path);

        // Log query parameters
        if (!rc.request().params().isEmpty()) {
            log.debug("  📋 Query Params: {}", rc.request().params().entries());
        }

        // Log path parameters
        if (!rc.pathParams().isEmpty()) {
            log.debug("  🔗 Path Params: {}", rc.pathParams());
        }

        // Log headers importantes
        log.debug("  📧 Headers: Content-Type={}, Accept={}, User-Agent={}",
            rc.request().getHeader("Content-Type"),
            rc.request().getHeader("Accept"),
            rc.request().getHeader("User-Agent"));

        // Log del body si existe
        if (rc.body() != null && rc.body().length() > 0) {
            String bodyStr = rc.body().asString();
            // Limitar el tamaño del log
            if (bodyStr.length() > 1000) {
                bodyStr = bodyStr.substring(0, 1000) + "... (truncated)";
            }
            log.debug("  📦 Request Body: {}", bodyStr);
        }
    }

    private void logOutgoingResponse(RoutingContext rc, String requestId, String traceId, long duration) {
        int statusCode = rc.response().getStatusCode();
        String statusMessage = getStatusEmoji(statusCode);

        log.info("{} RESPONSE [reqId:{}] [trace:{}] Status: {} - Duration: {} ms - Path: {}",
            statusMessage, requestId, traceId, statusCode, duration, rc.request().path());

        // Log response headers importantes
        log.debug("  📧 Response Headers: Content-Type={}",
            rc.response().headers().get("Content-Type"));

        // Si hay error, intentar obtener el body
        if (statusCode >= 400) {
            log.error("  ❌ Error Response - Status: {} - Path: {} - Duration: {} ms - Method: {}",
                statusCode, rc.request().path(), duration, rc.request().method().name());
        }
    }

    private String getStatusEmoji(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) return "✅";
        if (statusCode >= 300 && statusCode < 400) return "↪️";
        if (statusCode >= 400 && statusCode < 500) return "⚠️";
        if (statusCode >= 500) return "❌";
        return "ℹ️";
    }

    private String generateRequestId() {
        return String.format("%s-%d",
            Instant.now().toEpochMilli(),
            Thread.currentThread().getId());
    }
}
