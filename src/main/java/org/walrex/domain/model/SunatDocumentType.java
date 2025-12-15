package org.walrex.domain.model;

import lombok.*;

import java.time.OffsetDateTime;

/**
 * Modelo de dominio para tipos de documentos SUNAT.
 *
 * Representa un tipo de documento de identidad según la clasificación de SUNAT.
 * Este modelo es agnóstico a la persistencia y contiene solo lógica de negocio.
 *
 * Ejemplos:
 * - DNI: id='01', code='1', length=8, pattern='^[0-9]{8}$'
 * - RUC: id='06', code='6', length=11, pattern='^(10|20)[0-9]{9}$'
 * - Carné de Extranjería: id='07', code='4', length=12
 */
@Builder
@Data
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class SunatDocumentType {

    /**
     * Identificador único del tipo de documento.
     */
    private Integer id;

    /**
     * Código SUNAT del documento.
     */
    private String code;

    /**
     * Nombre descriptivo del tipo de documento.
     */
    private String name;

    /**
     * Descripción adicional del tipo de documento.
     */
    private String description;

    /**
     * Longitud exacta que debe tener el documento.
     */
    private Integer length;

    /**
     * Patrón regex para validar el formato del documento.
     */
    private String pattern;

    /**
     * Fecha de última actualización desde SUNAT.
     */
    private OffsetDateTime sunatUpdatedAt;

    /**
     * Estado activo/inactivo del tipo de documento.
     */
    private Boolean active;

    /**
     * Timestamp de creación del registro.
     */
    private OffsetDateTime createdAt;

    /**
     * Timestamp de última actualización del registro.
     */
    private OffsetDateTime updatedAt;

    /**
     * Valida si un número de documento cumple con el patrón definido.
     *
     * @param documentNumber Número de documento a validar
     * @return true si es válido, false en caso contrario
     */
    public boolean validateDocument(String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            return false;
        }

        // Validar longitud si está definida
        if (length != null && documentNumber.length() != length) {
            return false;
        }

        // Validar patrón si está definido
        if (pattern != null && !pattern.isBlank()) {
            return documentNumber.matches(pattern);
        }

        return true;
    }

    /**
     * Verifica si el tipo de documento está activo.
     *
     * @return true si está activo, false en caso contrario
     */
    public boolean isActive() {
        return active != null && active;
    }
}
