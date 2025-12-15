package org.walrex.application.dto.response;

import java.time.OffsetDateTime;

/**
 * Response DTO para tipos de documentos SUNAT.
 * Contiene toda la información del tipo de documento incluyendo timestamps.
 */
public record SunatDocumentTypeResponse(
        /**
         * Identificador único del tipo de documento.
         * Ejemplo: '01', '06', '07', '11'
         */
        String id,

        /**
         * Código SUNAT del documento.
         * Ejemplo: '1', '6', '4', '7'
         */
        String code,

        /**
         * Nombre descriptivo del tipo de documento.
         * Ejemplo: 'DNI', 'RUC', 'Carné de Extranjería'
         */
        String name,

        /**
         * Descripción adicional del tipo de documento.
         * Ejemplo: 'Documento Nacional de Identidad'
         */
        String description,

        /**
         * Longitud exacta del documento.
         * Ejemplo: DNI = 8, RUC = 11
         */
        Integer length,

        /**
         * Patrón regex para validar el formato del documento.
         * Ejemplo: '^[0-9]{8}$' para DNI
         */
        String pattern,

        /**
         * Fecha de última actualización desde SUNAT.
         * Se actualiza cuando se sincroniza con los datos oficiales.
         */
        OffsetDateTime sunatUpdatedAt,

        /**
         * Indica si el tipo de documento está activo según SUNAT.
         * true = activo, false = descontinuado
         */
        Boolean active,

        /**
         * Timestamp de creación del registro.
         */
        OffsetDateTime createdAt,

        /**
         * Timestamp de última actualización del registro.
         */
        OffsetDateTime updatedAt
) {
}
