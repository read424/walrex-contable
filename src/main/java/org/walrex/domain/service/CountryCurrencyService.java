package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.response.CountryCurrencyResponse;
import org.walrex.application.port.input.ManageCountryCurrencyUseCase;
import org.walrex.application.port.output.CountryCurrencyRepositoryPort;
import org.walrex.domain.exception.CountryNotFoundException;
import org.walrex.domain.exception.CurrencyNotFoundException;
import org.walrex.domain.model.CountryCurrency;
import org.walrex.infrastructure.adapter.inbound.mapper.CountryCurrencyDtoMapper;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Transactional
@ApplicationScoped
public class CountryCurrencyService implements ManageCountryCurrencyUseCase {

    @Inject
    CountryCurrencyRepositoryPort repositoryPort;

    @Inject
    CountryCurrencyDtoMapper mapper;

    @Override
    public Uni<List<CountryCurrencyResponse>> listCurrenciesByCountry(Integer countryId) {
        log.info("Listing currencies for country id: {}", countryId);
        return repositoryPort.findByCountryId(countryId)
                .onItem().transform(currencies -> currencies.stream()
                        .map(mapper::toResponse)
                        .toList());
    }

    @Override
    public Uni<CountryCurrencyResponse> assignCurrency(Integer countryId, Integer currencyId) {
        log.info("Assigning currency {} to country {}", currencyId, countryId);

        // Verificar si ya existe la relación
        return repositoryPort.existsByCountryIdAndCurrencyId(countryId, currencyId)
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Currency already assigned to this country")
                        );
                    }

                    // Crear la nueva relación
                    CountryCurrency countryCurrency = CountryCurrency.builder()
                            .countryId(countryId)
                            .currencyId(currencyId)
                            .isPrimary(false)
                            .isOperational(true)
                            .effectiveDate(LocalDate.now())
                            .build();

                    return repositoryPort.save(countryCurrency)
                            .onItem().transform(mapper::toResponse);
                });
    }

    @Override
    public Uni<CountryCurrencyResponse> setDefaultCurrency(Integer countryId, Integer currencyId) {
        log.info("Setting currency {} as default for country {}", currencyId, countryId);

        return repositoryPort.findByCountryIdAndCurrencyId(countryId, currencyId)
                .onItem().transformToUni(optionalRelation -> {
                    if (optionalRelation.isEmpty()) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Currency not assigned to this country")
                        );
                    }

                    CountryCurrency newPrimaryRelation = optionalRelation.get();

                    // Si ya es la moneda predeterminada, no hacer nada
                    if (Boolean.TRUE.equals(newPrimaryRelation.getIsPrimary())) {
                        return Uni.createFrom().item(mapper.toResponse(newPrimaryRelation));
                    }

                    // Buscar la moneda que actualmente es predeterminada
                    return repositoryPort.findPrimaryByCountryId(countryId)
                            .onItem().transformToUni(optionalCurrentPrimary -> {
                                Uni<Void> unsetCurrentPrimary;

                                // Si existe una moneda predeterminada, desactivarla primero
                                if (optionalCurrentPrimary.isPresent()) {
                                    CountryCurrency currentPrimary = optionalCurrentPrimary.get();
                                    currentPrimary.setIsPrimary(false);
                                    unsetCurrentPrimary = repositoryPort.update(currentPrimary)
                                            .replaceWithVoid();
                                } else {
                                    // No hay moneda predeterminada actualmente
                                    unsetCurrentPrimary = Uni.createFrom().voidItem();
                                }

                                // Después de desactivar la anterior, activar la nueva
                                return unsetCurrentPrimary.onItem().transformToUni(v -> {
                                    newPrimaryRelation.setIsPrimary(true);
                                    return repositoryPort.update(newPrimaryRelation)
                                            .onItem().transform(mapper::toResponse);
                                });
                            });
                });
    }

    @Override
    public Uni<CountryCurrencyResponse> updateOperationalStatus(Integer countryId, Integer currencyId, Boolean isOperational) {
        log.info("Updating operational status of currency {} for country {} to: {}",
                currencyId, countryId, isOperational);

        return repositoryPort.updateOperationalStatus(countryId, currencyId, isOperational)
                .onItem().transform(mapper::toResponse);
    }

    @Override
    public Uni<CountryCurrencyResponse> getDefaultCurrency(Integer countryId) {
        log.info("Getting default currency for country id: {}", countryId);
        return repositoryPort.findPrimaryByCountryId(countryId)
                .onItem().transform(optional -> optional
                        .map(mapper::toResponse)
                        .orElseThrow(() -> new CurrencyNotFoundException(
                                "No default currency found for country id: " + countryId))
                );
    }
}