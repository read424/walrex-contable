package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductCategoryUom;

/**
 * Puerto de entrada para crear categorías de unidades de medida.
 *
 * Siguiendo el patrón hexagonal, este puerto define el contrato
 * para el caso de uso de creación de categorías.
 */
public interface CreateProductCategoryUomUseCase {
    /**
     * Crea una nueva categoría de unidad de medida en el sistema.
     *
     * @param productCategoryUom Datos necesarios para crear la categoría
     * @return Uni con la categoría creada
     * @throws org.walrex.domain.exception.DuplicateProductCategoryUomException
     *         si ya existe una categoría con el mismo código o nombre
     */
    Uni<ProductCategoryUom> execute(ProductCategoryUom productCategoryUom);
}
