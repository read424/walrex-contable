package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.SystemDocumentType;

/**
 * Use case for updating an existing System Document Type.
 */
public interface UpdateSystemDocumentTypeUseCase {

    /**
     * Updates an existing system document type.
     *
     * @param id the unique identifier of the system document type to update
     * @param systemDocumentType the updated system document type data
     * @return Uni emitting the updated system document type
     * @throws org.walrex.domain.exception.SystemDocumentTypeNotFoundException if not found
     * @throws org.walrex.domain.exception.DuplicateSystemDocumentTypeException if code or name conflicts
     */
    Uni<SystemDocumentType> execute(Long id, SystemDocumentType systemDocumentType);
}
