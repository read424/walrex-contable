package org.walrex.infrastructure.adapter.inbound.rest.filter;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

/**
 * Filtro CORS para rutas Vert.x.
 *
 * Este filtro agrega los headers CORS necesarios para permitir peticiones
 * desde el frontend React u otros clientes web.
 *
 * Se ejecuta antes de todas las rutas (@RouteFilter con prioridad alta).
 *
 * Configuración en application.yml:
 * - quarkus.http.cors.origins
 * - quarkus.http.cors.methods
 * - quarkus.http.cors.headers
 */
@ApplicationScoped
public class CorsFilter {

    @ConfigProperty(name = "quarkus.http.cors.origins", defaultValue = "*")
    String allowedOrigins;

    @ConfigProperty(name = "quarkus.http.cors.methods", defaultValue = "GET, POST, PUT, DELETE, OPTIONS, PATCH")
    String allowedMethods;

    @ConfigProperty(name = "quarkus.http.cors.headers", defaultValue = "accept, authorization, content-type, x-requested-with")
    String allowedHeaders;

    @ConfigProperty(name = "quarkus.http.cors.exposed-headers")
    Optional<String> exposedHeaders;

    @ConfigProperty(name = "quarkus.http.cors.access-control-max-age", defaultValue = "86400")
    String maxAge;

    @ConfigProperty(name = "quarkus.http.cors.access-control-allow-credentials", defaultValue = "false")
    boolean allowCredentials;

    /**
     * Filtro que se ejecuta para TODAS las peticiones.
     * Prioridad 0 = se ejecuta primero, antes que los handlers de rutas.
     */
    @RouteFilter(0)
    void corsFilter(RoutingContext rc) {
        var response = rc.response();
        var request = rc.request();

        // Obtener el origen de la petición
        String origin = request.getHeader("Origin");

        // Si no hay origen, continuar (peticiones no-CORS como Swagger UI local)
        if (origin == null) {
            rc.next();
            return;
        }

        // Validar si el origen está permitido
        String allowOriginHeader = getAllowedOriginHeader(origin);

        // Headers CORS
        response.putHeader("Access-Control-Allow-Origin", allowOriginHeader);
        response.putHeader("Access-Control-Allow-Methods", allowedMethods);
        response.putHeader("Access-Control-Allow-Headers", allowedHeaders);

        exposedHeaders.ifPresent(headers ->
                response.putHeader("Access-Control-Expose-Headers", headers)
        );

        response.putHeader("Access-Control-Max-Age", maxAge);

        if (allowCredentials) {
            response.putHeader("Access-Control-Allow-Credentials", "true");
        }

        // Si es una petición OPTIONS (preflight), responder inmediatamente
        if ("OPTIONS".equals(request.method().name())) {
            response.setStatusCode(204); // No Content
            response.end();
            return;
        }

        // Continuar con el siguiente handler
        rc.next();
    }

    /**
     * Determina qué valor poner en Access-Control-Allow-Origin.
     *
     * Si allowedOrigins es "*", devuelve "*".
     * Si no, valida que el origin esté en la lista permitida.
     */
    private String getAllowedOriginHeader(String origin) {
        if ("*".equals(allowedOrigins.trim())) {
            return "*";
        }

        // Verificar si el origen está en la lista permitida
        String[] origins = allowedOrigins.split(",");
        for (String allowed : origins) {
            if (allowed.trim().equals(origin)) {
                return origin; // Devolver el origen específico
            }
        }

        // Si no está permitido, no agregar el header (el navegador bloqueará)
        return allowedOrigins.split(",")[0].trim(); // Devolver el primero por defecto
    }
}