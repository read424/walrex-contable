package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductCategoryUom;

/**
 * Puerto de entrada para obtener categorías de unidades de medida.
 */
public interface GetProductCategoryUomByIdUseCase {
    /**
     * Obtiene una categoría por su ID.
     *
     * @param id Identificador único de la categoría
     * @return Uni con la categoría encontrada
     * @throws org.walrex.domain.exception.ProductCategoryUomNotFoundException
     *         si no existe una categoría con el ID proporcionado
     */
    Uni<ProductCategoryUom> findById(Integer id);

    /**
     * Obtiene una categoría por su código único.
     *
     * @param code Código único de la categoría (ej: "LENGTH", "WEIGHT")
     * @return Uni con la categoría encontrada
     * @throws org.walrex.domain.exception.ProductCategoryUomNotFoundException
     *         si no existe una categoría con el código proporcionado
     */
    Uni<ProductCategoryUom> findByCode(String code);
}
