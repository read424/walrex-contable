package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.SystemDocumentType;

/**
 * Use case for retrieving a single System Document Type by ID.
 */
public interface GetSystemDocumentTypeUseCase {

    Uni<SystemDocumentType> findById(Long id);

    Uni<SystemDocumentType> findByCode(String code);
}
