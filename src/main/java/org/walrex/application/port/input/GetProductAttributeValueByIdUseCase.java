package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductAttributeValue;

/**
 * Puerto de entrada (Input Port) para obtener un valor de atributo de producto por su ID.
 *
 * Este caso de uso encapsula la lógica para consultar un valor de atributo específico:
 * 1. Buscar valor de atributo por ID
 * 2. Lanzar excepción si no existe o está eliminado
 *
 * Patrón Hexagonal:
 * Este puerto es DEFINIDO en la capa de aplicación e IMPLEMENTADO
 * por el servicio de dominio.
 */
public interface GetProductAttributeValueByIdUseCase {

    /**
     * Obtiene un valor de atributo de producto por su ID.
     *
     * @param id ID del valor de atributo a buscar (Integer)
     * @return Uni con el valor de atributo encontrado
     * @throws org.walrex.domain.exception.ProductAttributeValueNotFoundException si no existe el valor de atributo
     */
    Uni<ProductAttributeValue> findById(Integer id);

    /**
     * Obtiene un valor de atributo de producto por su nombre.
     *
     * @param name Nombre del valor de atributo a buscar
     * @return Uni con el valor de atributo encontrado
     * @throws org.walrex.domain.exception.ProductAttributeValueNotFoundException si no existe el valor de atributo
     */
    Uni<ProductAttributeValue> findByName(String name);
}
