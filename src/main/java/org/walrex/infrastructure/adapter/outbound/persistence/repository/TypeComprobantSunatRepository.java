package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.TypeComprobantSunatEntity;

import java.util.List;

@ApplicationScoped
public class TypeComprobantSunatRepository implements PanacheRepositoryBase<TypeComprobantSunatEntity, Integer> {

    /**
     * Obtiene todos los tipos de comprobantes SUNAT ordenados por código SUNAT.
     */
    public Uni<List<TypeComprobantSunatEntity>> findAllSorted() {
        return findAll(Sort.by("sunatCode").ascending()).list();
    }
}
