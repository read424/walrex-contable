package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductBrandEntity;

import java.util.List;

@ApplicationScoped
public class ProductBrandRepository implements PanacheRepositoryBase<ProductBrandEntity, Integer> {

    /**
     * Encuentra todas las marcas de producto ordenadas por nombre.
     *
     * @return Uni con lista de marcas de producto ordenadas por nombre
     */
    public Uni<List<ProductBrandEntity>> findAllOrdered() {
        return find("order by name").list();
    }
}
