package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductTemplate;

/**
 * Caso de uso para obtener una plantilla de producto por ID.
 */
public interface GetProductTemplateByIdUseCase {

    /**
     * Obtiene una plantilla de producto por su ID.
     *
     * @param id ID de la plantilla
     * @return Uni con la plantilla de producto
     * @throws org.walrex.domain.exception.ProductTemplateNotFoundException si no existe
     */
    Uni<ProductTemplate> findById(Integer id);

    /**
     * Obtiene una plantilla de producto por su referencia interna.
     *
     * @param internalReference Referencia interna única
     * @return Uni con la plantilla de producto
     * @throws org.walrex.domain.exception.ProductTemplateNotFoundException si no existe
     */
    Uni<ProductTemplate> findByInternalReference(String internalReference);
}
