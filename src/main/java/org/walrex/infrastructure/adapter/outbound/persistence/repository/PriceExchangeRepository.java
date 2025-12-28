package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.PriceExchangeEntity;

import java.time.LocalDate;

/**
 * Repositorio reactivo para PriceExchangeEntity
 */
@ApplicationScoped
public class PriceExchangeRepository implements PanacheRepositoryBase<PriceExchangeEntity, Integer> {

    /**
     * Desactiva tasas anteriores para el mismo par de monedas y tipo de operación
     */
    public Uni<Integer> deactivateOldRates(Integer idCurrencyBase, Integer idCurrencyQuote, String typeOperation) {
        return update("isActive = '0' WHERE idCurrencyBase = ?1 AND idCurrencyQuote = ?2 AND typeOperation = ?3 AND isActive = '1'",
                idCurrencyBase, idCurrencyQuote, typeOperation);
    }

    /**
     * Busca la última tasa activa para un par de monedas
     */
    public Uni<PriceExchangeEntity> findLatestActiveRate(Integer idCurrencyBase, Integer idCurrencyQuote, String typeOperation) {
        return find("idCurrencyBase = ?1 AND idCurrencyQuote = ?2 AND typeOperation = ?3 AND isActive = '1' ORDER BY createdAt DESC",
                idCurrencyBase, idCurrencyQuote, typeOperation)
                .firstResult();
    }

    /**
     * Busca tasas activas para una fecha específica
     */
    public Uni<PriceExchangeEntity> findByDateAndCurrencies(LocalDate date, Integer idCurrencyBase, Integer idCurrencyQuote, String typeOperation) {
        return find("dateExchange = ?1 AND idCurrencyBase = ?2 AND idCurrencyQuote = ?3 AND typeOperation = ?4 AND isActive = '1'",
                date, idCurrencyBase, idCurrencyQuote, typeOperation)
                .firstResult();
    }

    /**
     * Desactiva tasas de una fecha específica para el mismo par de monedas
     * Esto permite crear un nuevo registro activo para el mismo día
     */
    public Uni<Integer> deactivateRatesByDate(LocalDate date, Integer idCurrencyBase, Integer idCurrencyQuote, String typeOperation) {
        return update("isActive = '0' WHERE dateExchange = ?1 AND idCurrencyBase = ?2 AND idCurrencyQuote = ?3 AND typeOperation = ?4 AND isActive = '1'",
                date, idCurrencyBase, idCurrencyQuote, typeOperation);
    }
}
