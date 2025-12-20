package org.walrex.application.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Estructura de error estándar")
public record ErrorResponse(
        @Schema(description = "Código HTTP", example = "404") int status,
        @Schema(description = "Descripción del error HTTP", example = "Not Found") String error,
        @Schema(description = "Mensaje detallado", example = "El recurso no existe") String message) {
}
