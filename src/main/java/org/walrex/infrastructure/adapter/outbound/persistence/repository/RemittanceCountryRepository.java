package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CountryEntity;

import java.util.List;

@ApplicationScoped
public class RemittanceCountryRepository {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    public Uni<List<CountryEntity>> findAllAvailableCountries() {
        return Panache.withSession(() -> 
            sessionFactory.withSession(session ->
                session.createNativeQuery("""
                    SELECT c.id, c.code_iso2, c.code_iso3, c.name_iso, c.unicode_flag, 
                           c.code_phone_iso, c.status, c.created_at, c.updated_at, 
                           c.deleted_at, c.numeric_code
                    FROM remittance_countries rc
                    INNER JOIN country c ON c.id = rc.id_country AND c.status = '1'
                    WHERE rc.is_active = '1'
                    ORDER BY c.name_iso ASC
                    """, CountryEntity.class)
                    .getResultList()
            )
        );
    }
}