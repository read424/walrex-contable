package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductCategoryUom;

/**
 * Puerto de entrada para actualizar categorías de unidades de medida.
 */
public interface UpdateProductCategoryUomUseCase {

    /**
     * Actualiza una categoría existente con nuevos datos.
     *
     * @param id Identificador de la categoría a actualizar
     * @param productCategoryUom Nuevos datos para la categoría
     * @return Uni con la categoría actualizada
     * @throws org.walrex.domain.exception.ProductCategoryUomNotFoundException
     *         si no existe una categoría con el ID proporcionado
     * @throws org.walrex.domain.exception.DuplicateProductCategoryUomException
     *         si los nuevos datos entran en conflicto con otra categoría existente
     */
    Uni<ProductCategoryUom> execute(Integer id, ProductCategoryUom productCategoryUom);
}
