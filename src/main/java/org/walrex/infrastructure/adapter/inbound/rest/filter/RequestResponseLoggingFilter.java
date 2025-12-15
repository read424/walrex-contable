package org.walrex.infrastructure.adapter.inbound.rest.filter;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

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
     * Intercepta todas las requests antes de que lleguen al handler.
     * Priority 1 = se ejecuta antes que otros filtros.
     */
    @RouteFilter(1)
    void logRequest(RoutingContext rc) {
        long startTime = System.currentTimeMillis();
        String requestId = generateRequestId();

        // Guardar en el context para usarlo después
        rc.put("requestId", requestId);
        rc.put("startTime", startTime);

        // Log de información del request
        logIncomingRequest(rc, requestId);

        // Continuar con el siguiente handler
        rc.next();
    }

    /**
     * Intercepta la respuesta después de que el handler haya procesado.
     * Priority 10 = se ejecuta después que otros filtros.
     */
    @RouteFilter(10)
    void logResponse(RoutingContext rc) {
        // Agregar listener para cuando la respuesta termine
        rc.addEndHandler(result -> {
            String requestId = rc.get("requestId");
            Long startTime = rc.get("startTime");

            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                logOutgoingResponse(rc, requestId, duration);
            }
        });

        rc.next();
    }

    private void logIncomingRequest(RoutingContext rc, String requestId) {
        String method = rc.request().method().name();
        String uri = rc.request().uri();
        String path = rc.request().path();

        log.info("🔵 INCOMING REQUEST [{}] {} {} - Path: {}",
            requestId, method, uri, path);

        // Log query parameters
        if (!rc.request().params().isEmpty()) {
            log.debug("  📋 Query Params [{}]: {}", requestId, rc.request().params().entries());
        }

        // Log path parameters
        if (!rc.pathParams().isEmpty()) {
            log.debug("  🔗 Path Params [{}]: {}", requestId, rc.pathParams());
        }

        // Log headers importantes
        log.debug("  📧 Headers [{}]: Content-Type={}, Accept={}, User-Agent={}",
            requestId,
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
            log.debug("  📦 Request Body [{}]: {}", requestId, bodyStr);
        }
    }

    private void logOutgoingResponse(RoutingContext rc, String requestId, long duration) {
        int statusCode = rc.response().getStatusCode();
        String statusMessage = getStatusEmoji(statusCode);

        log.info("{} RESPONSE [{}] Status: {} - Duration: {} ms",
            statusMessage, requestId, statusCode, duration);

        // Log response headers importantes
        log.debug("  📧 Response Headers [{}]: Content-Type={}",
            requestId, rc.response().headers().get("Content-Type"));

        // Si hay error, intentar obtener el body
        if (statusCode >= 400) {
            log.error("  ❌ Error Response [{}] - Status: {} - Path: {} - Duration: {} ms",
                requestId, statusCode, rc.request().path(), duration);
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
