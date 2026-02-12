package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.*;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class RemittanceExchangeRateRepository implements PanacheRepository<PriceExchangeEntity> {

    /**
     * Obtiene las tasas más recientes para un par de monedas específico
     */
    public Uni<List<PriceExchangeEntity>> findLatestRatesByCurrencyPair(Integer baseCurrencyId, Integer quoteCurrencyId) {
        return find("""
            SELECT pe FROM PriceExchangeEntity pe 
            WHERE pe.idCurrencyBase = ?1 
            AND pe.idCurrencyQuote = ?2 
            AND pe.isActive = '1'
            AND pe.dateExchange = (
                SELECT MAX(pe2.dateExchange) 
                FROM PriceExchangeEntity pe2 
                WHERE pe2.idCurrencyBase = ?1 
                AND pe2.idCurrencyQuote = ?2 
                AND pe2.isActive = '1'
            )
            ORDER BY pe.exchangeRateTypeId ASC
            """, baseCurrencyId, quoteCurrencyId).list();
    }

    /**
     * Obtiene todas las tasas disponibles para una moneda base específica
     */
    public Uni<List<PriceExchangeEntity>> findLatestRatesByBaseCurrency(Integer baseCurrencyId) {
        return find("""
            SELECT pe FROM PriceExchangeEntity pe 
            WHERE pe.idCurrencyBase = ?1 
            AND pe.isActive = '1'
            AND pe.dateExchange = (
                SELECT MAX(pe2.dateExchange) 
                FROM PriceExchangeEntity pe2 
                WHERE pe2.idCurrencyBase = ?1 
                AND pe2.idCurrencyQuote = pe.idCurrencyQuote
                AND pe2.isActive = '1'
            )
            ORDER BY pe.idCurrencyQuote ASC, pe.exchangeRateTypeId ASC
            """, baseCurrencyId).list();
    }

    /**
     * Obtiene tasas con información completa de monedas y tipos
     * Método simplificado que retorna solo las tasas y luego se mapean en el adaptador
     */
    public Uni<List<PriceExchangeEntity>> findRatesWithCurrencyAndTypeInfo(Integer baseCurrencyId, Integer quoteCurrencyId) {
        return find("""
            SELECT pe
            FROM PriceExchangeEntity pe
            WHERE pe.idCurrencyBase = ?1 
            AND pe.idCurrencyQuote = ?2 
            AND pe.isActive = '1'
            AND pe.dateExchange = (
                SELECT MAX(pe2.dateExchange) 
                FROM PriceExchangeEntity pe2 
                WHERE pe2.idCurrencyBase = ?1 
                AND pe2.idCurrencyQuote = ?2 
                AND pe2.isActive = '1'
            )
            ORDER BY pe.exchangeRateSourceId ASC
            """, baseCurrencyId, quoteCurrencyId).list();
    }

    /**
     * Obtiene todas las tasas disponibles desde una moneda base con información completa
     * Método simplificado que retorna solo las tasas y luego se mapean en el adaptador
     */
    public Uni<List<PriceExchangeEntity>> findAllRatesFromBaseCurrencyWithInfo(Integer baseCurrencyId) {
        return find("""
            SELECT pe
            FROM PriceExchangeEntity pe
            WHERE pe.idCurrencyBase = ?1 
            AND pe.isActive = '1'
            AND pe.dateExchange = (
                SELECT MAX(pe2.dateExchange) 
                FROM PriceExchangeEntity pe2 
                WHERE pe2.idCurrencyBase = ?1 
                AND pe2.idCurrencyQuote = pe.idCurrencyQuote
                AND pe2.isActive = '1'
            )
            ORDER BY pe.idCurrencyQuote ASC, pe.exchangeRateSourceId ASC
            """, baseCurrencyId).list();
    }

    /**
     * Actualiza o inserta una nueva tasa
     */
    public Uni<PriceExchangeEntity> upsertRate(Integer baseCurrencyId, Integer quoteCurrencyId, 
                                              Long exchangeRateSourceId, java.math.BigDecimal rate) {
        LocalDate today = LocalDate.now();
        
        return find("""
            idCurrencyBase = ?1 AND idCurrencyQuote = ?2 
            AND exchangeRateSourceId = ?3 AND dateExchange = ?4
            """, baseCurrencyId, quoteCurrencyId, exchangeRateSourceId, today)
            .firstResult()
            .onItem().ifNull().continueWith(() -> {
                PriceExchangeEntity newRate = PriceExchangeEntity.builder()
                    .typeOperation("3") // REMESAS
                    .idCurrencyBase(baseCurrencyId)
                    .idCurrencyQuote(quoteCurrencyId)
                    .amountPrice(rate)
                    .dateExchange(today)
                    .isActive("1")
                    .build();
                return newRate;
            })
            .onItem().transform(entity -> {
                entity.setAmountPrice(rate);
                entity.setIsActive("1");
                return entity;
            })
            .call(entity -> persist(entity));
    }
}