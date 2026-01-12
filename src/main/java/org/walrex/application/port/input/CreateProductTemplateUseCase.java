package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductTemplate;

/**
 * Caso de uso para crear una nueva plantilla de producto.
 */
public interface CreateProductTemplateUseCase {

    /**
     * Crea una nueva plantilla de producto.
     *
     * Validaciones realizadas:
     * - Unicidad de referencia interna (si se proporciona)
     * - Existencia de categoría (si se proporciona)
     * - Existencia de marca (si se proporciona)
     * - Existencia de unidad de medida (requerido)
     * - Existencia de moneda (requerido)
     * - Aplicación de reglas específicas por tipo de producto
     *
     * @param productTemplate Plantilla de producto a crear
     * @return Uni con la plantilla creada
     */
    Uni<ProductTemplate> execute(ProductTemplate productTemplate);
}
