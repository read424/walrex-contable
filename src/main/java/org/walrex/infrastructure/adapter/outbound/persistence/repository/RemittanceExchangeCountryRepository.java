package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;
import org.walrex.infrastructure.adapter.outbound.persistence.dto.RemittanceRouteResultDto;

import java.util.List;

@ApplicationScoped
public class RemittanceExchangeCountryRepository {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    public Uni<List<RemittanceRouteResultDto>> findDestinationCountriesWithCurrencies(Integer countryId) {
        return Panache.withSession(() -> 
            sessionFactory.withSession(session ->
                session.createNativeQuery("""
                    SELECT c.code_iso3 AS codeIsoFrom, 
                           c.name AS nameIsoFrom, 
                           c.symbol AS symbolIsoFrom, 
                           c2.code_iso3 AS codeIsoTo, 
                           c2.name AS nameIsoTo, 
                           c2.symbol AS symbolIsoTo, 
                           c3.code_iso2 AS countryIso2, 
                           c3.code_iso3 AS countryIso3, 
                           c3.name_iso AS countryName, 
                           c3.unicode_flag AS countryFlag
                    FROM remittance_countries rc
                    LEFT OUTER JOIN remittance_routes rr ON rr.id_remittance_country = rc.id
                    LEFT OUTER JOIN country_currencies cc ON cc.id = rr.id_country_currencies_from
                    LEFT OUTER JOIN currencies c ON c.id = cc.currency_id
                    LEFT OUTER JOIN country_currencies cc2 ON cc2.id = rr.id_country_currencies_to
                    LEFT OUTER JOIN currencies c2 ON c2.id = cc2.currency_id
                    LEFT OUTER JOIN country c3 ON c3.id = cc2.country_id
                    WHERE rc.id_country = :countryId AND rc.is_active = '1'
                    ORDER BY rr.id
                    """)
                    .setParameter("countryId", countryId)
                    .getResultList()
            )
        ).onItem().transform(results -> {
            List<RemittanceRouteResultDto> dtoList = new java.util.ArrayList<>();
            for (Object result : results) {
                Object[] row = (Object[]) result;
                dtoList.add(new RemittanceRouteResultDto(
                    (String) row[0], // codeIsoFrom
                    (String) row[1], // nameIsoFrom
                    (String) row[2], // symbolIsoFrom
                    (String) row[3], // codeIsoTo
                    (String) row[4], // nameIsoTo
                    (String) row[5], // symbolIsoTo
                    (String) row[6], // countryIso2
                    (String) row[7], // countryIso3
                    (String) row[8], // countryName
                    (String) row[9]  // countryFlag
                ));
            }
            return dtoList;
        });
    }
}