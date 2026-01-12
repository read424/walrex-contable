package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductAttribute;

/**
 * Puerto de entrada para actualizar atributos de producto.
 *
 * Siguiendo el patrón hexagonal, este puerto define el contrato
 * para el caso de uso de actualización de atributos.
 */
public interface UpdateProductAttributeUseCase {
    /**
     * Actualiza un atributo de producto existente.
     *
     * @param id Identificador del atributo a actualizar (Integer)
     * @param productAttribute Datos actualizados del atributo
     * @return Uni con el atributo actualizado
     * @throws org.walrex.domain.exception.ProductAttributeNotFoundException
     *         si no existe un atributo con el id proporcionado
     * @throws org.walrex.domain.exception.DuplicateProductAttributeException
     *         si el nuevo nombre ya existe en otro atributo
     */
    Uni<ProductAttribute> execute(Integer id, ProductAttribute productAttribute);
}
