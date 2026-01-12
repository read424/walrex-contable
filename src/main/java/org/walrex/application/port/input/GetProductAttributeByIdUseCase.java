package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductAttribute;

/**
 * Puerto de entrada para obtener un atributo de producto por su ID.
 *
 * Siguiendo el patrón hexagonal, este puerto define el contrato
 * para el caso de uso de consulta de atributo por ID.
 */
public interface GetProductAttributeByIdUseCase {
    /**
     * Obtiene un atributo de producto por su identificador.
     *
     * @param id Identificador del atributo (Integer)
     * @return Uni con el atributo encontrado
     * @throws org.walrex.domain.exception.ProductAttributeNotFoundException
     *         si no existe un atributo con el id proporcionado
     */
    Uni<ProductAttribute> findById(Integer id);

    /**
     * Obtiene un atributo de producto por su nombre.
     *
     * @param name Nombre del atributo
     * @return Uni con el atributo encontrado
     * @throws org.walrex.domain.exception.ProductAttributeNotFoundException
     *         si no existe un atributo con el nombre proporcionado
     */
    Uni<ProductAttribute> findByName(String name);
}
