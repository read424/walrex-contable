package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductVariantEntity;

/**
 * Repositorio para operaciones de persistencia de ProductVariant.
 *
 * Usa el patrón Repository de Panache que proporciona:
 * - Operaciones CRUD básicas (persist, delete, findById, etc.)
 * - Queries tipadas
 * - Manejo reactivo con Uni/Multi
 *
 * Este es un repositorio MINIMAL ya que ProductVariant es gestionado internamente
 * por ProductTemplateService y no requiere métodos de búsqueda complejos.
 * Solo se usa para persistencia básica.
 */
@ApplicationScoped
public class ProductVariantRepository implements PanacheRepositoryBase<ProductVariantEntity, Integer> {
    // No custom methods needed - Panache base operations are sufficient
}
