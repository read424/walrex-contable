package org.walrex.infrastructure.adapter.inbound.rest.security;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Interceptor de seguridad JWT para rutas Vert.x.
 *
 * SmallRye JWT no integra automáticamente con rutas Vert.x (@Route),
 * por lo que este interceptor valida manualmente el token JWT
 * desde el header Authorization: Bearer {token}.
 *
 * Uso en handlers:
 * <pre>
 *   Optional<JsonWebToken> tokenOpt = jwtSecurityInterceptor.authenticate(rc);
 *   if (tokenOpt.isEmpty()) return Uni.createFrom().voidItem(); // ya envió 401
 *   Integer userId = jwtSecurityInterceptor.getUserId(tokenOpt.get());
 * </pre>
 */
@Slf4j
@ApplicationScoped
public class JwtSecurityInterceptor {

    @Inject
    JWTParser jwtParser;

    @ConfigProperty(name = "mp.jwt.secret")
    String secret;

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Autentica la petición extrayendo y validando el JWT del header Authorization.
     * Si falla, envía la respuesta 401 directamente al cliente.
     *
     * @param rc RoutingContext de la petición
     * @return Optional con el token validado, o empty si la autenticación falló (respuesta ya enviada)
     */
    public Optional<JsonWebToken> authenticate(RoutingContext rc) {
        String authHeader = rc.request().getHeader(AUTH_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Missing or malformed Authorization header from {}", rc.request().remoteAddress());
            sendUnauthorized(rc, "Missing or invalid authorization header");
            return Optional.empty();
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            SecretKey secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            JsonWebToken jwt = jwtParser.verify(token, secretKey);
            log.debug("JWT validated successfully for subject: {}", jwt.getSubject());
            return Optional.of(jwt);
        } catch (ParseException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            sendUnauthorized(rc, "Invalid or expired token");
            return Optional.empty();
        }
    }

    /**
     * Extrae el userId (subject) del token JWT.
     *
     * @param jwt Token validado
     * @return userId como Integer
     * @throws NumberFormatException si el subject no es un número
     */
    public Integer getUserId(JsonWebToken jwt) {
        return Integer.valueOf(jwt.getSubject());
    }

    private void sendUnauthorized(RoutingContext rc, String message) {
        JsonObject error = new JsonObject()
                .put("status", 401)
                .put("error", "Unauthorized")
                .put("message", message)
                .put("path", rc.request().path());

        rc.response()
                .setStatusCode(401)
                .putHeader("Content-Type", "application/json")
                .end(error.encode());
    }
}
