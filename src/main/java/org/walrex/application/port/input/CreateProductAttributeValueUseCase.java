package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductAttributeValue;

/**
 * Puerto de entrada (Input Port) para crear un nuevo valor de atributo de producto.
 *
 * Este caso de uso encapsula la lógica de negocio para crear valores de atributos:
 * 1. Validar formato del ID (lowercase, alphanumeric + underscore)
 * 2. Validar que el attributeId exista
 * 3. Validar unicidad del ID
 * 4. Validar unicidad de la combinación (attributeId, name)
 * 5. Validar formato del htmlColor si se proporciona
 * 6. Setear timestamps (createdAt, updatedAt)
 * 7. Guardar en base de datos
 * 8. Invalidar caché
 *
 * Patrón Hexagonal:
 * Este puerto es DEFINIDO en la capa de aplicación e IMPLEMENTADO
 * por el servicio de dominio.
 */
public interface CreateProductAttributeValueUseCase {

    /**
     * Crea un nuevo valor de atributo de producto.
     *
     * @param productAttributeValue Valor de atributo a crear (debe incluir id proporcionado por cliente)
     * @return Uni con el valor de atributo creado
     * @throws org.walrex.domain.exception.InvalidProductAttributeValueIdException si el id no cumple formato
     * @throws org.walrex.domain.exception.DuplicateProductAttributeValueException si id o (attributeId+name) ya existe
     * @throws org.walrex.domain.exception.ProductAttributeNotFoundException si attributeId no existe
     */
    Uni<ProductAttributeValue> execute(ProductAttributeValue productAttributeValue);
}
