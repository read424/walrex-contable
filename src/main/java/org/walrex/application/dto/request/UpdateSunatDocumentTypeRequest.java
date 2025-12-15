package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para actualizar un tipo de documento SUNAT existente.
 * No incluye el ID porque se pasa como path parameter.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSunatDocumentTypeRequest {

    /**
     * Código SUNAT del documento.
     * Ejemplo: '1', '6', '4', '7'
     */
    @NotBlank(message = "El código es obligatorio")
    @Size(max = 10, message = "El código no puede exceder 10 caracteres")
    private String code;

    /**
     * Nombre descriptivo del tipo de documento.
     * Ejemplo: 'DNI - Documento Nacional de Identidad'
     */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String name;

    /**
     * Descripción adicional del tipo de documento.
     * Ejemplo: 'Documento Nacional de Identidad emitido por RENIEC'
     */
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String description;

    /**
     * Longitud exacta del documento.
     * Ejemplo: DNI = 8, RUC = 11
     */
    @Positive(message = "La longitud debe ser un número positivo")
    private Integer length;

    /**
     * Patrón regex para validar el formato del documento.
     * Ejemplo: '^[0-9]{8}$' para DNI
     */
    @Size(max = 50, message = "El patrón no puede exceder 50 caracteres")
    private String pattern;

    /**
     * Indica si el tipo de documento está activo.
     * true = activo, false = inactivo/descontinuado
     */
    private Boolean active;
}
