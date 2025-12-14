package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.SystemDocumentType;

/**
 * Use case for creating a new System Document Type.
 */
public interface CreateSystemDocumentTypeUseCase {

    Uni<SystemDocumentType> agregar(SystemDocumentType systemDocumentType);
}
