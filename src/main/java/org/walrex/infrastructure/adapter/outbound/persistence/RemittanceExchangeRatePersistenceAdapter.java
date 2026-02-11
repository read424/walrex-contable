package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.walrex.application.port.output.RemittanceExchangeRateOutputPort;
import org.walrex.domain.model.Country;
import org.walrex.domain.model.Currency;
import org.walrex.domain.model.ExchangeRateSource;
import org.walrex.domain.model.RemittanceExchangeRate;
import org.walrex.infrastructure.adapter.outbound.persistence.dto.ExchangeRateWithDetailsDto;
import org.walrex.infrastructure.adapter.outbound.persistence.dto.RemittanceRouteResultDto;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.*;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ExchangeRateTypeEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.CountryMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.CurrencyMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.ExchangeRateMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.CountryCurrencyRepository;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.CountryRepository;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.ExchangeRateTypeRepository;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.RemittanceCountryRepository;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.RemittanceExchangeCountryRepository;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.RemittanceExchangeRateRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
@RequiredArgsConstructor
public class RemittanceExchangeRatePersistenceAdapter implements RemittanceExchangeRateOutputPort {

    @Inject
    RemittanceExchangeRateRepository exchangeRateRepository;

    @Inject
    RemittanceCountryRepository remittanceCountryRepository;

    @Inject
    RemittanceExchangeCountryRepository remittanceExchangeCountryRepository;

    @Inject
    CountryRepository countryRepository;

    @Inject
    CountryCurrencyRepository countryCurrencyRepository;

    @Inject
    ExchangeRateTypeRepository exchangeRateTypeRepository;

    @Inject
    ExchangeRateMapper exchangeRateMapper;

    @Inject
    CountryMapper countryMapper;

    @Inject
    CurrencyMapper currencyMapper;

    @Override
    public Uni<List<Country>> findAllAvailableCountries() {
        return remittanceCountryRepository.findAllAvailableCountries()
            .onItem().transform(entities -> entities.stream()
                .map(countryMapper::toDomain)
                .collect(Collectors.toList()));
    }

    @Override
    public Uni<List<Currency>> findOperationalCurrenciesByCountry(Integer countryId) {
        return countryCurrencyRepository.findByCountryIdWithCurrency(countryId)
                .onItem().transform(entities -> entities.stream()
                    .filter(cc -> cc.getIsOperational())
                    .map(cc -> currencyMapper.toDomain(cc.getCurrency()))
                    .collect(Collectors.toList()));
    }

    @Override
    public Uni<List<ExchangeRateSource>> findAllActiveExchangeRateSources() {
        return exchangeRateTypeRepository.findAllActive()
            .onItem().transform(entities -> entities.stream()
                .map(exchangeRateMapper::toDomain)
                .collect(Collectors.toList()));
    }

    @Override
    public Uni<List<RemittanceExchangeRate>> findLatestRatesByCurrencyPair(Long baseCurrencyId, Long quoteCurrencyId) {
        return exchangeRateRepository.findLatestRatesByCurrencyPair(baseCurrencyId.intValue(), quoteCurrencyId.intValue())
            .onItem().transform(entities -> entities.stream()
                .map(exchangeRateMapper::toDomain)
                .collect(Collectors.toList()));
    }

    @Override
    public Uni<Map<Country, Map<Currency, Map<ExchangeRateSource, BigDecimal>>>> findAllRatesFromBaseCurrency(Long baseCurrencyId) {
        // Por ahora retornamos un mapa vacío ya que este método se usa en el servicio actual
        // pero no es necesario para el nuevo endpoint que estamos implementando
        return Uni.createFrom().item(new HashMap<>());
    }

    /**
     * Nuevo método para obtener países destino con sus monedas para remesas
     */
    @Override
    public Uni<List<RemittanceRouteResultDto>> findDestinationCountriesWithCurrencies(Integer countryId) {
        return remittanceExchangeCountryRepository.findDestinationCountriesWithCurrencies(countryId);
    }

    @Override
    public Uni<RemittanceExchangeRate> upsertExchangeRate(Long baseCurrencyId, Long quoteCurrencyId, 
                                                         Long exchangeRateSourceId, BigDecimal rate) {
        return exchangeRateRepository.upsertRate(baseCurrencyId.intValue(), quoteCurrencyId.intValue(), 
                                               exchangeRateSourceId, rate)
            .onItem().transform(exchangeRateMapper::toDomain);
    }

    @Override
    public Uni<Country> findCountryByIso2(String iso2) {
        return countryRepository.findByIso2Enabled(iso2)
            .onItem().ifNotNull().transform(countryMapper::toDomain);
    }

    @Override
    public Uni<Currency> findCurrencyByCode(String code) {
        return CurrencyEntity.<CurrencyEntity>find("alphabeticCode = ?1 AND status = '1'", code).firstResult()
            .onItem().ifNotNull().transform(currencyMapper::toDomain);
    }

    @Override
    public Uni<ExchangeRateSource> findExchangeRateSourceByCode(String code) {
        return exchangeRateTypeRepository.findByCode(code)
            .onItem().ifNotNull().transform(exchangeRateMapper::toDomain);
    }

    @Override
    public Uni<List<org.walrex.domain.model.ExchangeRateType>> findActiveRateTypesByCountry(Integer countryId) {
        // Usar consulta personalizada con JOIN FETCH para cargar las relaciones
        return ExchangeRateTypeEntity.<ExchangeRateTypeEntity>find("""
            SELECT ert FROM ExchangeRateTypeEntity ert 
            LEFT JOIN FETCH ert.country c 
            LEFT JOIN FETCH ert.baseCurrency bc 
            WHERE ert.countryId = ?1 AND ert.isActive = '1' 
            ORDER BY ert.displayOrder ASC
            """, countryId)
            .list()
            .onItem().transform(entities -> entities.stream()
                .map(this::mapToExchangeRateTypeDomain)
                .collect(Collectors.toList()));
    }

    @Override
    public Uni<org.walrex.domain.model.ExchangeRateType> findRateTypeByCountryAndCode(Integer countryId, String rateCode) {
        // Usar consulta personalizada con JOIN FETCH para cargar las relaciones
        return ExchangeRateTypeEntity.<ExchangeRateTypeEntity>find("""
            SELECT ert FROM ExchangeRateTypeEntity ert 
            LEFT JOIN FETCH ert.country c 
            LEFT JOIN FETCH ert.baseCurrency bc 
            WHERE ert.countryId = ?1 AND ert.codeRate = ?2 AND ert.isActive = '1'
            """, countryId, rateCode)
            .firstResult()
            .onItem().ifNotNull().transform(this::mapToExchangeRateTypeDomain);
    }

    private Map<Country, Map<Currency, Map<ExchangeRateSource, BigDecimal>>> buildRatesMap(List<ExchangeRateWithDetailsDto> results) {
        Map<Country, Map<Currency, Map<ExchangeRateSource, BigDecimal>>> ratesMap = new HashMap<>();
        
        for (ExchangeRateWithDetailsDto result : results) {
            PriceExchangeEntity priceEntity = result.getPriceExchange();
            CurrencyEntity quoteCurrency = result.getQuoteCurrency();
            ExchangeRateTypeEntity rateSource = result.getExchangeRateSource();
            CountryEntity country = result.getCountry();
            
            Country countryDomain = countryMapper.toDomain(country);
            Currency currencyDomain = currencyMapper.toDomain(quoteCurrency);
            ExchangeRateSource rateSourceDomain = rateSource != null ? exchangeRateMapper.toDomain(rateSource) : null;
            
            ratesMap.computeIfAbsent(countryDomain, k -> new HashMap<>())
                   .computeIfAbsent(currencyDomain, k -> new HashMap<>())
                   .put(rateSourceDomain, priceEntity.getAmountPrice());
        }
        
        return ratesMap;
    }

    private org.walrex.domain.model.ExchangeRateType mapToExchangeRateTypeDomain(ExchangeRateTypeEntity entity) {
        return org.walrex.domain.model.ExchangeRateType.builder()
            .id(entity.getId())
            .countryId(entity.getCountryId())
            .dateRate(entity.getDateRate())
            .codeRate(entity.getCodeRate())
            .nameRate(entity.getNameRate())
            .rateValue(entity.getRateValue())
            .baseCurrencyId(entity.getBaseCurrencyId())
            .status(entity.getIsActive())
            .displayOrder(entity.getDisplayOrder())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .country(entity.getCountry() != null ? countryMapper.toDomain(entity.getCountry()) : null)
            .baseCurrency(entity.getBaseCurrency() != null ? currencyMapper.toDomain(entity.getBaseCurrency()) : null)
            .build();
    }
}