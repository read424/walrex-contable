package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.InstitutionPayoutRailEntity;

import java.util.List;

@ApplicationScoped
public class InstitutionPayoutRailRepository implements PanacheRepositoryBase<InstitutionPayoutRailEntity, Long> {

    public Uni<List<InstitutionPayoutRailEntity>> findByRailCodeAndCountryIso2(String railCode, String countryIso2) {
        return find("SELECT ipr FROM InstitutionPayoutRailEntity ipr " +
                "JOIN FETCH ipr.bank b " +
                "JOIN FETCH ipr.payoutRail pr " +
                "JOIN CountryEntity c ON b.idCountry = c.id " +
                "WHERE pr.code = ?1 AND c.alphabeticCode2 = ?2 AND ipr.status = '1'", 
                railCode, countryIso2).list();
    }
}
