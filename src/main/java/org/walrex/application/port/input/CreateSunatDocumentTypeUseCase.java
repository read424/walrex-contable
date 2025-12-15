package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.SunatDocumentType;

/**
 * Caso de uso para crear un nuevo tipo de documento SUNAT.
 *
 * Este puerto define el contrato para la creación de tipos de documentos.
 * La implementación se encuentra en la capa de dominio (Service).
 */
public interface CreateSunatDocumentTypeUseCase {
    /**
     * Crea un nuevo tipo de documento SUNAT en el sistema.
     *
     * @param documentType Datos necesarios para crear el tipo de documento
     * @return Uni con el tipo de documento creado (incluye timestamps)
     * @throws org.walrex.domain.exception.DuplicateSunatDocumentTypeException
     *         si ya existe un tipo de documento con el mismo ID o código
     * @throws org.walrex.domain.exception.InvalidSunatDocumentTypeDataException
     *         si los datos no cumplen las reglas de negocio
     */
    Uni<SunatDocumentType> create(SunatDocumentType documentType);
}
