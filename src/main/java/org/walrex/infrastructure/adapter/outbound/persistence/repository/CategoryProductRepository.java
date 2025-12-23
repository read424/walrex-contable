package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CategoryProductEntity;

import java.util.List;

@ApplicationScoped
public class CategoryProductRepository implements PanacheRepositoryBase<CategoryProductEntity, Integer> {

    /**
     * Encuentra todas las categorías raíz (sin padre).
     *
     * @return Uni con lista de categorías raíz ordenadas por nombre
     */
    public Uni<List<CategoryProductEntity>> findRootCategories() {
        return find("parent is null order by name").list();
    }

    /**
     * Encuentra todas las categorías hijas de un padre específico.
     *
     * @param parentId ID de la categoría padre
     * @return Uni con lista de categorías hijas ordenadas por nombre
     */
    public Uni<List<CategoryProductEntity>> findByParentId(Integer parentId) {
        return find("parent.id = ?1 order by name", parentId).list();
    }
}
