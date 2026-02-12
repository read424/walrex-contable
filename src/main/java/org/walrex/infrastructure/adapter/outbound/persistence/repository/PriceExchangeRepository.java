package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.PriceExchangeEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

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

    @Inject
    Mutiny.SessionFactory sessionFactory;

    /**
     * Busca la tasa activa más reciente por códigos de país y moneda usando native SQL.
     * Joins: price_exchange -> country_currencies -> country + currencies
     */
    public Uni<Optional<BigDecimal>> findActiveRateByCountriesAndCurrencies(
            String fromCountryCode, String fromCurrencyCode,
            String toCountryCode, String toCurrencyCode) {

        String sql = """
                SELECT pe.amount_price
                FROM price_exchange pe
                INNER JOIN country_currencies cc_from ON cc_from.id = pe.id_currency_base
                INNER JOIN country c_from ON c_from.id = cc_from.country_id AND c_from.status = '1'
                INNER JOIN currencies cur_from ON cur_from.id = cc_from.currency_id
                INNER JOIN country_currencies cc_to ON cc_to.id = pe.id_currency_quote
                INNER JOIN country c_to ON c_to.id = cc_to.country_id AND c_to.status = '1'
                INNER JOIN currencies cur_to ON cur_to.id = cc_to.currency_id
                WHERE c_from.code_iso2 = :fromCountry
                AND cur_from.code_iso3 = :fromCurrency
                AND c_to.code_iso2 = :toCountry
                AND cur_to.code_iso3 = :toCurrency
                AND pe.is_active = '1'
                ORDER BY pe.date_exchange DESC
                LIMIT 1
                """;

        return sessionFactory.withSession(session ->
                session.createNativeQuery(sql, BigDecimal.class)
                        .setParameter("fromCountry", fromCountryCode)
                        .setParameter("fromCurrency", fromCurrencyCode)
                        .setParameter("toCountry", toCountryCode)
                        .setParameter("toCurrency", toCurrencyCode)
                        .getSingleResultOrNull()
                        .map(Optional::ofNullable)
        );
    }
}
