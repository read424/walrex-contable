package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductVariantValueRelEntity;

import java.util.List;

/**
 * Repositorio para operaciones de persistencia de ProductVariantValueRel.
 *
 * Maneja la relación many-to-many entre variantes y valores de atributos.
 */
@ApplicationScoped
public class ProductVariantValueRelRepository
        implements PanacheRepositoryBase<ProductVariantValueRelEntity, ProductVariantValueRelEntity.CompositeKey> {

    /**
     * Encuentra todas las relaciones (valores de atributos) de una variante.
     *
     * @param variantId ID de la variante
     * @return Uni con lista de relaciones
     */
    public Uni<List<ProductVariantValueRelEntity>> findByVariantId(Integer variantId) {
        return list("SELECT r FROM ProductVariantValueRelEntity r " +
                        "LEFT JOIN FETCH r.attributeValue " +
                        "WHERE r.variantId = ?1",
                variantId);
    }

    /**
     * Elimina todas las relaciones de una variante.
     *
     * @param variantId ID de la variante
     * @return Uni con número de registros eliminados
     */
    public Uni<Long> deleteByVariantId(Integer variantId) {
        return delete("variantId = ?1", variantId);
    }

    /**
     * Encuentra todas las relaciones de múltiples variantes.
     *
     * @param variantIds Lista de IDs de variantes
     * @return Uni con lista de relaciones
     */
    public Uni<List<ProductVariantValueRelEntity>> findByVariantIds(List<Integer> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) {
            return Uni.createFrom().item(List.of());
        }

        return list("SELECT r FROM ProductVariantValueRelEntity r " +
                        "LEFT JOIN FETCH r.attributeValue " +
                        "WHERE r.variantId IN (?1)",
                variantIds);
    }
}
