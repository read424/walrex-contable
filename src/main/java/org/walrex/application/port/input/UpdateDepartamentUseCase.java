package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Departament;

public interface UpdateDepartamentUseCase {
    /**
     * Actualiza un departamento existente.
     *
     * @param id          ID del departamento a actualizar
     * @param departament Datos actualizados
     * @return Uni con el departamento actualizado
     */
    Uni<Departament> execute(Integer id, Departament departament);
}
