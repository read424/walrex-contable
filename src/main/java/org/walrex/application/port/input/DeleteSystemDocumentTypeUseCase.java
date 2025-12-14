package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

/**
 * Use case for deleting a System Document Type (soft delete).
 */
public interface DeleteSystemDocumentTypeUseCase {
    Uni<Boolean> deshabilitar(Long id);

    Uni<Boolean> habilitar(Long id);
}
