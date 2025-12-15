package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.SystemDocumentType;

/**
 * Puerto de entrada para crear un tipo de documento del sistema.
 */
public interface CreateSystemDocumentTypeUseCase {
    Uni<SystemDocumentType> agregar(SystemDocumentType systemDocumentType);
}
