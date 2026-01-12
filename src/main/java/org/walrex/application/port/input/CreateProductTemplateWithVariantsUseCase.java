package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.request.CreateProductTemplateRequest;
import org.walrex.domain.model.ProductTemplate;

/**
 * Puerto de entrada (caso de uso) para crear una plantilla de producto con variantes.
 *
 * Este caso de uso maneja la creación atómica de:
 * 1. ProductTemplate (producto base)
 * 2. ProductTemplateAttributeLine (atributos asignados)
 * 3. ProductVariant (variantes del producto)
 * 4. ProductVariantValueRel (combinaciones de atributos por variante)
 *
 * Todo en una sola transacción para garantizar consistencia.
 */
public interface CreateProductTemplateWithVariantsUseCase {

    /**
     * Crea un producto completo con sus variantes.
     *
     * Proceso:
     * 1. Validar que los atributos existen
     * 2. Validar que los valores de atributos existen
     * 3. Validar que las combinaciones son únicas
     * 4. Crear ProductTemplate
     * 5. Crear líneas de atributos (template-attribute relationship)
     * 6. Crear variantes
     * 7. Crear relaciones variante-valores
     *
     * @param request DTO con todos los datos del producto y variantes
     * @return Uni con el ProductTemplate creado
     */
    Uni<ProductTemplate> execute(CreateProductTemplateRequest request);
}
