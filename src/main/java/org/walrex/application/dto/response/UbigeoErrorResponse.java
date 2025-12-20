package org.walrex.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO para respuestas de error en las operaciones de Ubigeo.
 *
 * Proporciona información estructurada sobre errores HTTP específicos
 * del módulo de Ubigeo (carga INEI, validaciones, etc.).
 *
 * Ejemplo de uso:
 * <pre>
 * UbigeoErrorResponse error = UbigeoErrorResponse.builder()
 *     .error("Algunos códigos UBIGEO ya existen")
 *     .message("El distrito con código 150101 ya existe en la base de datos")
 *     .path("/api/v1/ubigeo/inei/load-save")
 *     .status(409)
 *     .build();
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UbigeoErrorResponse {

    /**
     * Mensaje de error principal (por ejemplo: "No se enviaron registros", "Algunos códigos UBIGEO ya existen")
     */
    private String error;

    /**
     * Mensaje descriptivo adicional del error (opcional)
     */
    private String message;

    /**
     * Timestamp de cuando ocurrió el error
     */
    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();

    /**
     * Path del endpoint donde ocurrió el error
     */
    private String path;

    /**
     * Código de estado HTTP
     */
    private Integer status;

    /**
     * Lista de detalles adicionales del error (útil para validaciones múltiples)
     */
    private List<String> details;

    /**
     * Constructor de conveniencia para errores simples con solo un mensaje.
     *
     * @param error Mensaje de error
     */
    public UbigeoErrorResponse(String error) {
        this.error = error;
        this.timestamp = OffsetDateTime.now();
    }

    /**
     * Constructor de conveniencia para errores con código y mensaje.
     *
     * @param error   Mensaje de error principal
     * @param message Mensaje descriptivo adicional
     */
    public UbigeoErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.timestamp = OffsetDateTime.now();
    }

    /**
     * Método estático para crear una respuesta de error simple.
     *
     * @param error Mensaje de error
     * @return UbigeoErrorResponse con solo el mensaje
     */
    public static UbigeoErrorResponse of(String error) {
        return new UbigeoErrorResponse(error);
    }

    /**
     * Método estático para crear una respuesta de error con mensaje adicional.
     *
     * @param error   Mensaje de error principal
     * @param message Mensaje descriptivo adicional
     * @return UbigeoErrorResponse completo
     */
    public static UbigeoErrorResponse of(String error, String message) {
        return new UbigeoErrorResponse(error, message);
    }
}
