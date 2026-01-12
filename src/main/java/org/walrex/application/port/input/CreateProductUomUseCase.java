package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductUom;

/**
 * Puerto de entrada para crear unidades de medida de productos.
 *
 * Siguiendo el patrón hexagonal, este puerto define el contrato
 * para el caso de uso de creación de unidades de medida.
 */
public interface CreateProductUomUseCase {
    /**
     * Crea una nueva unidad de medida de producto en el sistema.
     *
     * @param productUom Datos necesarios para crear la unidad de medida
     * @return Uni con la unidad de medida creada
     * @throws org.walrex.domain.exception.DuplicateProductUomException
     *         si ya existe una unidad con el mismo código
     * @throws org.walrex.domain.exception.ProductCategoryUomNotFoundException
     *         si la categoría especificada no existe
     */
    Uni<ProductUom> execute(ProductUom productUom);
}
