package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductAttribute;

/**
 * Puerto de entrada para crear atributos de producto.
 *
 * Siguiendo el patrón hexagonal, este puerto define el contrato
 * para el caso de uso de creación de atributos.
 */
public interface CreateProductAttributeUseCase {
    /**
     * Crea un nuevo atributo de producto en el sistema.
     *
     * @param productAttribute Datos necesarios para crear el atributo
     * @return Uni con el atributo creado
     * @throws org.walrex.domain.exception.DuplicateProductAttributeException
     *         si ya existe un atributo con el mismo id o nombre
     * @throws org.walrex.domain.exception.InvalidProductAttributeIdException
     *         si el id no cumple con el formato requerido
     */
    Uni<ProductAttribute> execute(ProductAttribute productAttribute);
}
