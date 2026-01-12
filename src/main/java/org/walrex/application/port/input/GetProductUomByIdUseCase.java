package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductUom;

/**
 * Puerto de entrada para obtener unidades de medida de productos por ID.
 *
 * Siguiendo el patrón hexagonal, este puerto define el contrato
 * para el caso de uso de consulta individual de unidades de medida.
 */
public interface GetProductUomByIdUseCase {
    /**
     * Obtiene una unidad de medida de producto por su identificador.
     *
     * @param id ID de la unidad de medida
     * @return Uni con la unidad de medida encontrada
     * @throws org.walrex.domain.exception.ProductUomNotFoundException
     *         si no existe una unidad con el ID proporcionado
     */
    Uni<ProductUom> findById(Integer id);

    /**
     * Obtiene una unidad de medida de producto por su código único.
     *
     * @param code Código de la unidad de medida
     * @return Uni con la unidad de medida encontrada
     * @throws org.walrex.domain.exception.ProductUomNotFoundException
     *         si no existe una unidad con el código proporcionado
     */
    Uni<ProductUom> findByCode(String code);
}
