package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductUom;

/**
 * Puerto de entrada para actualizar unidades de medida de productos.
 *
 * Siguiendo el patrón hexagonal, este puerto define el contrato
 * para el caso de uso de actualización de unidades de medida.
 */
public interface UpdateProductUomUseCase {
    /**
     * Actualiza una unidad de medida de producto existente.
     *
     * @param id ID de la unidad de medida a actualizar
     * @param productUom Datos actualizados de la unidad de medida
     * @return Uni con la unidad de medida actualizada
     * @throws org.walrex.domain.exception.ProductUomNotFoundException
     *         si no existe una unidad con el ID proporcionado
     * @throws org.walrex.domain.exception.DuplicateProductUomException
     *         si el nuevo código ya existe en otra unidad
     * @throws org.walrex.domain.exception.ProductCategoryUomNotFoundException
     *         si la categoría especificada no existe
     */
    Uni<ProductUom> execute(Integer id, ProductUom productUom);
}
