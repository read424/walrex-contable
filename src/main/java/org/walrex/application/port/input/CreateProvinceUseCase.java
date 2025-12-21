package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Province;

public interface CreateProvinceUseCase {
    /**
     * Crea una nueva provincia en el sistema.
     *
     * @param province Datos necesarios para crear la provincia
     * @return Uni con la provincia creada
     * @throws org.walrex.domain.exception.DuplicateProvinceException
     *         si ya existe una provincia con los mismos datos únicos
     * @throws org.walrex.domain.exception.DepartamentNotFoundException
     *         si el departamento asociado no existe
     */
    Uni<Province> agregar(Province province);
}
