package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductTemplate;

/**
 * Caso de uso para actualizar una plantilla de producto existente.
 */
public interface UpdateProductTemplateUseCase {

    /**
     * Actualiza una plantilla de producto existente.
     *
     * Validaciones realizadas:
     * - Existencia de la plantilla
     * - Unicidad de referencia interna excluyendo el propio ID
     * - Existencia de categoría (si se proporciona)
     * - Existencia de marca (si se proporciona)
     * - Existencia de unidad de medida (requerido)
     * - Existencia de moneda (requerido)
     * - Aplicación de reglas específicas por tipo de producto
     *
     * @param id ID de la plantilla a actualizar
     * @param productTemplate Datos actualizados de la plantilla
     * @return Uni con la plantilla actualizada
     */
    Uni<ProductTemplate> execute(Integer id, ProductTemplate productTemplate);
}
