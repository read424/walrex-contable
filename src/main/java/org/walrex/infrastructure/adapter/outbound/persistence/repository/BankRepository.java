package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.BankEntity;

import java.util.List;

/**
 * Repository para bancos/métodos de pago
 */
@ApplicationScoped
public class BankRepository implements PanacheRepositoryBase<BankEntity, Long> {

    /**
     * Find all active banks
     */
    public Uni<List<BankEntity>> findAllActive() {
        return find("status = ?1", "1").list();
    }

    /**
     * Find active banks by country
     */
    public Uni<List<BankEntity>> findActiveByCountry(Integer countryId) {
        return find("idCountry = ?1 and status = ?2", countryId, "1").list();
    }

    /**
     * Find bank by Binance payment code
     */
    public Uni<BankEntity> findByBinancePaymentCode(String namePayBinance) {
        return find("namePayBinance = ?1 and status = ?2", namePayBinance, "1")
                .firstResult();
    }
}
