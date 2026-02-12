package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import org.walrex.application.dto.response.CountriesListResponse;
import org.walrex.application.dto.response.CountryRatesResponse;
import org.walrex.application.dto.response.CountryRateTypesResponse;
import org.walrex.application.dto.response.RateCalculationResponse;
import org.walrex.application.dto.response.CountryInfoResponse;
import org.walrex.application.dto.response.CurrencyInfoResponse;
import org.walrex.application.dto.response.DestinationCountryResponse;
import org.walrex.application.dto.request.CalculateRateRequest;
import org.walrex.application.port.input.GetCountryExchangeRatesUseCase;
import org.walrex.application.port.input.GetRemittanceCountriesUseCase;
import org.walrex.application.port.input.GetCountryRateTypesUseCase;
import org.walrex.application.port.input.CalculateRateTypeUseCase;
import org.walrex.application.port.output.RemittanceExchangeRateOutputPort;
import org.walrex.domain.model.Country;
import org.walrex.domain.model.Currency;
import org.walrex.domain.model.ExchangeRateType;
import org.walrex.infrastructure.adapter.outbound.persistence.dto.RemittanceRouteResultDto;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.ExchangeRateMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.RemittanceCountryRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@ApplicationScoped
@RequiredArgsConstructor
public class RemittanceExchangeRateService implements GetRemittanceCountriesUseCase, GetCountryExchangeRatesUseCase, 
                                                     GetCountryRateTypesUseCase, CalculateRateTypeUseCase {

    @Inject
    RemittanceExchangeRateOutputPort exchangeRateOutputPort;

    @Inject
    RemittanceCountryRepository remittanceCountryRepository;

    @Inject
    ExchangeRateMapper mapper;

    @Override
    @WithSession
    public Uni<CountriesListResponse> getAvailableCountries() {
        return exchangeRateOutputPort.findAllAvailableCountries()
            .onItem().transform(countries -> {
                List<CountryInfoResponse> countryResponses = countries.stream()
                    .map(mapper::toCountryInfoResponse)
                    .collect(Collectors.toList());
                
                return CountriesListResponse.builder()
                    .countries(countryResponses)
                    .build();
            });
    }

    @Override
    @WithSession
    public Uni<CountryRatesResponse> getExchangeRatesByCountry(String countryIso2) {
        return exchangeRateOutputPort.findCountryByIso2(countryIso2)
            .onItem().ifNull().failWith(() -> new IllegalArgumentException("País no encontrado: " + countryIso2))
            .chain(originCountry -> {
                // Obtener monedas operacionales del país origen
                Uni<List<Currency>> originCurrencies = exchangeRateOutputPort.findOperationalCurrenciesByCountry(originCountry.getId());
                
                // Obtener países destino con sus monedas
                Uni<List<RemittanceRouteResultDto>> destinationsData = exchangeRateOutputPort.findDestinationCountriesWithCurrencies(originCountry.getId());
                
                return Uni.combine().all().unis(originCurrencies, destinationsData)
                    .asTuple()
                    .onItem().transform(tuple -> {
                        List<Currency> currencies = tuple.getItem1();
                        List<RemittanceRouteResultDto> destinations = tuple.getItem2();
                        
                        return buildCountryRatesResponse(originCountry, currencies, destinations);
                    });
            });
    }

    private CountryRatesResponse buildCountryRatesResponse(Country originCountry, 
                                                         List<Currency> originCurrencies,
                                                         List<RemittanceRouteResultDto> destinationsData) {
        
        CountryInfoResponse originCountryResponse = mapper.toCountryInfoResponse(originCountry);
        
        List<CurrencyInfoResponse> originCurrencyResponses = originCurrencies.stream()
            .map(mapper::toCurrencyInfoResponse)
            .collect(Collectors.toList());

        // Agrupar destinos por país (countryIso3)
        Map<String, List<RemittanceRouteResultDto>> groupedByCountry = destinationsData.stream()
            .filter(dto -> dto.getCountryIso2() != null) // Filtrar filas con datos de país destino
            .collect(Collectors.groupingBy(RemittanceRouteResultDto::getCountryIso3)); // Agrupar por countryIso3

        List<DestinationCountryResponse> destinations = new ArrayList<>();
        
        for (Map.Entry<String, List<RemittanceRouteResultDto>> entry : groupedByCountry.entrySet()) {
            List<RemittanceRouteResultDto> countryRows = entry.getValue();
            RemittanceRouteResultDto firstRow = countryRows.get(0);
            
            // Datos del país destino (tomar del primer registro)
            CountryInfoResponse destCountryResponse = CountryInfoResponse.builder()
                .iso2(firstRow.getCountryIso2())
                .iso3(firstRow.getCountryIso3())
                .name(firstRow.getCountryName())
                .flagEmoji(firstRow.getCountryFlag())
                .build();
            
            // Monedas del país destino (deduplicadas por código ISO)
            List<CurrencyInfoResponse> destCurrencies = countryRows.stream()
                .filter(dto -> dto.getCodeIsoTo() != null)
                .collect(Collectors.toMap(
                    RemittanceRouteResultDto::getCodeIsoTo,
                    dto -> CurrencyInfoResponse.builder()
                        .code(dto.getCodeIsoTo())
                        .name(dto.getNameIsoTo())
                        .symbol(dto.getSymbolIsoTo())
                        .build(),
                    (existing, duplicate) -> existing,
                    LinkedHashMap::new
                ))
                .values().stream().collect(Collectors.toList());

            // Cantidad de exchange_rate_types activos para el país destino
            Integer rateTypesCount = firstRow.getRateTypesCount() != null ? firstRow.getRateTypesCount() : 0;

            destinations.add(DestinationCountryResponse.builder()
                .country(destCountryResponse)
                .currencies(destCurrencies)
                .rateTypesCount(rateTypesCount)
                .build());
        }
        
        return CountryRatesResponse.builder()
            .originCountry(originCountryResponse)
            .originCurrencies(originCurrencyResponses)
            .destinations(destinations)
            .build();
    }

    @Override
    @WithSession
    public Uni<CountryRateTypesResponse> getRateTypesByCountry(String countryIso2) {
        return exchangeRateOutputPort.findCountryByIso2(countryIso2)
            .onItem().ifNull().failWith(() -> new IllegalArgumentException("País no encontrado: " + countryIso2))
            .chain(country -> {
                Uni<List<ExchangeRateType>> rateTypes = exchangeRateOutputPort.findActiveRateTypesByCountry(country.getId());
                
                return rateTypes.onItem().transform(types -> {
                    CountryInfoResponse countryResponse = mapper.toCountryInfoResponse(country);
                    
                    List<org.walrex.application.dto.response.ExchangeRateTypeResponse> rateTypeResponses = types.stream()
                        .map(this::mapToExchangeRateTypeResponse)
                        .collect(Collectors.toList());
                    
                    return CountryRateTypesResponse.builder()
                        .country(countryResponse)
                        .rateTypes(rateTypeResponses)
                        .build();
                });
            });
    }

    @Override
    @WithSession
    public Uni<RateCalculationResponse> calculateRate(CalculateRateRequest request) {
        return exchangeRateOutputPort.findCountryByIso2(request.getCountryIso2())
            .onItem().ifNull().failWith(() -> new IllegalArgumentException("País no encontrado: " + request.getCountryIso2()))
            .chain(country -> 
                exchangeRateOutputPort.findRateTypeByCountryAndCode(country.getId(), request.getRateCode())
                    .onItem().ifNull().failWith(() -> new IllegalArgumentException("Tipo de tasa no encontrado: " + request.getRateCode()))
                    .onItem().transform(rateType -> {
                        BigDecimal outputAmount = request.getAmount().multiply(rateType.getRateValue());
                        
                        RateCalculationResponse.RateUsed rateUsed = RateCalculationResponse.RateUsed.builder()
                            .code(rateType.getCodeRate())
                            .name(rateType.getNameRate())
                            .value(rateType.getRateValue())
                            .date(rateType.getDateRate().toString())
                            .build();
                        
                        RateCalculationResponse.CalculationResult calculation = RateCalculationResponse.CalculationResult.builder()
                            .inputAmount(request.getAmount())
                            .inputCurrency(request.getBaseCurrency())
                            .outputAmount(outputAmount)
                            .outputCurrency(country.getAlphabeticCode3()) // Asumiendo que es la moneda del país
                            .rateUsed(rateUsed)
                            .calculatedAt(OffsetDateTime.now())
                            .build();
                        
                        return RateCalculationResponse.builder()
                            .calculation(calculation)
                            .build();
                    })
            );
    }

    private org.walrex.application.dto.response.ExchangeRateTypeResponse mapToExchangeRateTypeResponse(ExchangeRateType rateType) {
        // Si tenemos la relación baseCurrency cargada, usamos su código, sino usamos "USD" por defecto
        String baseCurrencyCode = "USD"; // Valor por defecto
        if (rateType.getBaseCurrency() != null) {
            baseCurrencyCode = rateType.getBaseCurrency().getAlphabeticCode();
        }
        
        return org.walrex.application.dto.response.ExchangeRateTypeResponse.builder()
            .id(rateType.getId())
            .code(rateType.getCodeRate())
            .name(rateType.getNameRate())
            .rateValue(rateType.getRateValue())
            .baseCurrency(baseCurrencyCode)
            .dateRate(rateType.getDateRate())
            .displayOrder(rateType.getDisplayOrder())
            .build();
    }
}