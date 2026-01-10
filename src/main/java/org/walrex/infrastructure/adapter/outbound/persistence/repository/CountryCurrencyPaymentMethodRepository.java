package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CountryCurrencyPaymentMethodEntity;

import java.util.List;

/**
 * Repository para payment methods por country_currency
 */
@ApplicationScoped
public class CountryCurrencyPaymentMethodRepository
        implements PanacheRepositoryBase<CountryCurrencyPaymentMethodEntity, Long> {

    /**
     * Find all active payment methods for a specific country_currency
     * Returns with bank details eagerly loaded
     *
     * @param countryCurrencyId ID from country_currencies table
     * @return Uni with list of active payment method entities with bank details
     */
    public Uni<List<String>> findActiveByCountryCurrencyId(Long countryCurrencyId) {
        return find("select b.namePayBinance from CountryCurrencyPaymentMethodEntity ccpm " +
                   "join ccpm.bank b " +
                   "where ccpm.countryCurrency.id = ?1 " +
                   "and ccpm.isActive = '1' " +
                   "and b.status = '1' " +
                   "and b.namePayBinance is not null",
                   countryCurrencyId)
                .project(String.class)
                .list();
    }

    /**
     * Find all active payment methods for multiple country_currencies
     *
     * @param countryCurrencyIds List of country_currencies IDs
     * @return Uni with list of active payment method entities
     */
    public Uni<List<String>> findActiveByCountryCurrencyIds(List<Long> countryCurrencyIds) {
        if (countryCurrencyIds == null || countryCurrencyIds.isEmpty()) {
            return Uni.createFrom().item(List.of());
        }

        return find("select b.namePayBinance from CountryCurrencyPaymentMethodEntity ccpm " +
                   "join ccpm.bank b " +
                   "where ccpm.countryCurrency.id in (?1) " +
                   "and ccpm.isActive = '1' " +
                   "and b.status = '1' " +
                   "and b.namePayBinance is not null",
                   countryCurrencyIds)
                .project(String.class)
                .list();
    }
}
