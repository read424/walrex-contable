package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductAttributeValue;

/**
 * Puerto de entrada (Input Port) para actualizar un valor de atributo de producto existente.
 *
 * Este caso de uso encapsula la lógica de negocio para actualizar valores de atributos:
 * 1. Verificar que el valor de atributo existe
 * 2. Validar unicidad de la combinación (attributeId, name) excluyendo el ID actual
 * 3. Validar formato del htmlColor si se proporciona
 * 4. Actualizar updatedAt
 * 5. Guardar cambios en base de datos
 * 6. Invalidar caché
 *
 * NOTA: El attributeId NO se puede cambiar (integridad referencial).
 *
 * Patrón Hexagonal:
 * Este puerto es DEFINIDO en la capa de aplicación e IMPLEMENTADO
 * por el servicio de dominio.
 */
public interface UpdateProductAttributeValueUseCase {

    /**
     * Actualiza un valor de atributo de producto existente.
     *
     * @param id ID del valor de atributo a actualizar (Integer)
     * @param productAttributeValue Datos actualizados del valor de atributo
     * @return Uni con el valor de atributo actualizado
     * @throws org.walrex.domain.exception.ProductAttributeValueNotFoundException si no existe el valor de atributo
     * @throws org.walrex.domain.exception.DuplicateProductAttributeValueException si (attributeId+name) ya existe
     */
    Uni<ProductAttributeValue> execute(Integer id, ProductAttributeValue productAttributeValue);
}
