package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

public interface DeleteDepartamentUseCase {
    /**
     * Deshabilita (soft delete) un departamento.
     *
     * @param id Identificador del departamento
     * @return Uni<Void>
     */
    Uni<Void> deshabilitar(Integer id);
}
