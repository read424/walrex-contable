package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Province;

public interface UpdateProvinceUseCase {

    /**
     * Actualiza una provincia existente con nuevos datos.
     *
     * @param id Identificador de la provincia a actualizar
     * @param province Nuevos datos para la provincia
     * @return Uni con la provincia actualizada
     * @throws org.walrex.domain.exception.ProvinceNotFoundException
     *         si no existe una provincia con el ID proporcionado
     * @throws org.walrex.domain.exception.DuplicateProvinceException
     *         si los nuevos datos entran en conflicto con otra provincia existente
     */
    Uni<Province> execute(Integer id, Province province);
}
