package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Departament;

public interface CreateDepartamentUseCase {
    /**
     * Crea un nuevo departamento en el sistema.
     *
     * @param departament Datos necesarios para crear el departamento
     * @return Uni con el departamento creado
     */
    Uni<Departament> agregar(Departament departament);
}
