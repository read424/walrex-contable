package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.SunatDocumentType;

/**
 * Caso de uso para obtener un tipo de documento SUNAT por su identificador.
 */
public interface GetSunatDocumentTypeUseCase {
    /**
     * Obtiene un tipo de documento por su ID.
     *
     * @param id Identificador único del tipo de documento
     * @return Uni con el tipo de documento encontrado
     * @throws org.walrex.domain.exception.SunatDocumentTypeNotFoundException
     *         si no existe un tipo de documento con el ID proporcionado
     */
    Uni<SunatDocumentType> findById(Integer id);

    /**
     * Obtiene un tipo de documento por su código SUNAT.
     *
     * @param code Código SUNAT del tipo de documento
     * @return Uni con el tipo de documento encontrado
     * @throws org.walrex.domain.exception.SunatDocumentTypeNotFoundException
     *         si no existe un tipo de documento con el código proporcionado
     */
    Uni<SunatDocumentType> findByCode(String code);
}
