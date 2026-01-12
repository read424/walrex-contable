package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductTemplateAttributeLineEntity;

import java.util.List;

/**
 * Repositorio para operaciones de persistencia de ProductTemplateAttributeLine.
 *
 * Maneja la relación entre templates de producto y sus atributos.
 */
@ApplicationScoped
public class ProductTemplateAttributeLineRepository
        implements PanacheRepositoryBase<ProductTemplateAttributeLineEntity, Integer> {

    /**
     * Encuentra todas las líneas de atributos para un template específico.
     *
     * @param productTemplateId ID del template
     * @return Uni con lista de líneas de atributos
     */
    public Uni<List<ProductTemplateAttributeLineEntity>> findByProductTemplateId(Integer productTemplateId) {
        return list("SELECT l FROM ProductTemplateAttributeLineEntity l " +
                        "LEFT JOIN FETCH l.attribute " +
                        "WHERE l.productTemplateId = ?1",
                productTemplateId);
    }

    /**
     * Elimina todas las líneas de atributos de un template.
     *
     * @param productTemplateId ID del template
     * @return Uni con número de registros eliminados
     */
    public Uni<Long> deleteByProductTemplateId(Integer productTemplateId) {
        return delete("productTemplateId = ?1", productTemplateId);
    }

    /**
     * Verifica si existe una línea de atributo específica.
     *
     * @param productTemplateId ID del template
     * @param attributeId ID del atributo
     * @return Uni con true si existe, false si no
     */
    public Uni<Boolean> exists(Integer productTemplateId, Integer attributeId) {
        return count("productTemplateId = ?1 and attributeId = ?2", productTemplateId, attributeId)
                .map(count -> count > 0);
    }
}
