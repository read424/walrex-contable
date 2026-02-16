package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DocumentTypeIdEntity;

import java.util.List;

@ApplicationScoped
public class DocumentTypeIdRepository implements PanacheRepositoryBase<DocumentTypeIdEntity, Integer> {

    public Uni<List<DocumentTypeIdEntity>> findByCountryIso2(String countryIso2) {
        return find("SELECT d FROM DocumentTypeIdEntity d " +
                "JOIN FETCH d.country c " +
                "WHERE c.alphabeticCode2 = ?1 AND d.status = '1'", 
                countryIso2).list();
    }
}
