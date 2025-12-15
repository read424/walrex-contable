package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CountryCurrencyEntity;

import java.util.List;

@ApplicationScoped
public class CountryCurrencyRepository implements PanacheRepositoryBase<CountryCurrencyEntity, Long> {

    /**
     * Encuentra todas las monedas de un país.
     */
    public Uni<List<CountryCurrencyEntity>> findByCountryId(Integer countryId) {
        return find("country.id = ?1 order by isPrimary desc, currency.alphabeticCode asc", countryId)
                .list();
    }

    /**
     * Encuentra la moneda predeterminada de un país.
     */
    public Uni<CountryCurrencyEntity> findPrimaryByCountryId(Integer countryId) {
        return find("select cc from CountryCurrencyEntity cc " +
                        "join fetch cc.currency " +
                        "join fetch cc.country " +
                        "where cc.country.id = ?1 and cc.isPrimary = true", countryId)
                .firstResult();
    }

    /**
     * Encuentra una relación específica país-moneda.
     */
    public Uni<CountryCurrencyEntity> findByCountryIdAndCurrencyId(Integer countryId, Integer currencyId) {
        return find("select cc from CountryCurrencyEntity cc " +
                        "join fetch cc.currency " +
                        "join fetch cc.country " +
                        "where cc.country.id = ?1 and cc.currency.id = ?2", countryId, currencyId)
                .firstResult();
    }

    /**
     * Verifica si existe una relación país-moneda.
     */
    public Uni<Boolean> existsByCountryIdAndCurrencyId(Integer countryId, Integer currencyId) {
        return count("country.id = ?1 and currency.id = ?2", countryId, currencyId)
                .map(count -> count > 0);
    }

    /**
     * Actualiza el estado isPrimary de todas las monedas de un país a false.
     */
    public Uni<Integer> unsetAllPrimaryForCountry(Integer countryId) {
        return update("isPrimary = false where country.id = ?1 and isPrimary = true", countryId);
    }

    /**
     * Actualiza el estado isOperational de una relación país-moneda.
     */
    public Uni<Integer> updateOperationalStatus(Integer countryId, Integer currencyId, Boolean isOperational) {
        return update("isOperational = ?1 where country.id = ?2 and currency.id = ?3",
                isOperational, countryId, currencyId);
    }

    /**
     * Obtiene todas las monedas con sus datos completos (con joins).
     */
    public Uni<List<CountryCurrencyEntity>> findByCountryIdWithCurrency(Integer countryId) {
        return find("select cc from CountryCurrencyEntity cc " +
                        "join fetch cc.currency " +
                        "join fetch cc.country " +
                        "where cc.country.id = ?1 " +
                        "order by cc.isPrimary desc, cc.currency.alphabeticCode asc", countryId)
                .list();
    }
}