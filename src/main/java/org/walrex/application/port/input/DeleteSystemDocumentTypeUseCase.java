package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

/**
 * Puerto de entrada para eliminar (soft delete) un tipo de documento del
 * sistema.
 */
public interface DeleteSystemDocumentTypeUseCase {
    Uni<Boolean> deshabilitar(Long id);

    Uni<Boolean> habilitar(Long id);
}
