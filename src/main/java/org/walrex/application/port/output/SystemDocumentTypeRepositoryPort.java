package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.SystemDocumentType;

/**
 * Output port for System Document Type write operations.
 */
public interface SystemDocumentTypeRepositoryPort {
    Uni<SystemDocumentType> save(SystemDocumentType systemDocumentType);

    Uni<SystemDocumentType> update(SystemDocumentType systemDocumentType);

    Uni<Boolean> softDelete(Long id);

    Uni<Boolean> hardDelete(Long id);

    Uni<Boolean> restore(Long id);
}
