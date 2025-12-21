package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Province;

public interface GetProvinceUseCase {
    /**
     * Obtiene una provincia por su ID.
     *
     * @param id Identificador único de la provincia
     * @return Uni con la provincia encontrada
     * @throws org.walrex.domain.exception.ProvinceNotFoundException
     *         si no existe una provincia con el ID proporcionado
     */
    Uni<Province> findById(Integer id);

    /**
     * Obtiene una provincia por su código UBIGEO.
     *
     * @param code Código de 4 dígitos (ej: 0101, 1501)
     * @return Uni con la provincia encontrada
     * @throws org.walrex.domain.exception.ProvinceNotFoundException
     *         si no existe una provincia con el código proporcionado
     */
    Uni<Province> findByCode(String code);

    /**
     * Obtiene una provincia por su nombre.
     *
     * @param name Nombre de la provincia
     * @return Uni con la provincia encontrada
     * @throws org.walrex.domain.exception.ProvinceNotFoundException
     *         si no existe una provincia con el nombre proporcionado
     */
    Uni<Province> findByName(String name);
}
