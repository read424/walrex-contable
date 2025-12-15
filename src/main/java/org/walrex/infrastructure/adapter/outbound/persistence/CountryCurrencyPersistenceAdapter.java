package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.port.output.CountryCurrencyRepositoryPort;
import org.walrex.domain.model.CountryCurrency;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CountryCurrencyEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.CountryCurrencyMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.CountryCurrencyRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CountryCurrencyPersistenceAdapter implements CountryCurrencyRepositoryPort {

    @Inject
    CountryCurrencyRepository repository;

    @Inject
    CountryCurrencyMapper mapper;

    @Override
    public Uni<CountryCurrency> save(CountryCurrency countryCurrency) {
        CountryCurrencyEntity entity = mapper.toEntity(countryCurrency);
        entity.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return repository.persist(entity)
                .onItem().transformToUni(savedEntity ->
                        // Volver a buscar la entidad con las relaciones completas cargadas
                        repository.findByCountryIdAndCurrencyId(
                                countryCurrency.getCountryId(),
                                countryCurrency.getCurrencyId()
                        )
                )
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Uni<List<CountryCurrency>> findByCountryId(Integer countryId) {
        return repository.findByCountryIdWithCurrency(countryId)
                .onItem().transform(entities -> entities.stream()
                        .map(mapper::toDomain)
                        .toList());
    }

    @Override
    public Uni<Optional<CountryCurrency>> findPrimaryByCountryId(Integer countryId) {
        return repository.findPrimaryByCountryId(countryId)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<CountryCurrency>> findByCountryIdAndCurrencyId(Integer countryId, Integer currencyId) {
        return repository.findByCountryIdAndCurrencyId(countryId, currencyId)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Boolean> existsByCountryIdAndCurrencyId(Integer countryId, Integer currencyId) {
        return repository.existsByCountryIdAndCurrencyId(countryId, currencyId);
    }

    @Override
    public Uni<Void> unsetAllPrimaryForCountry(Integer countryId) {
        return repository.unsetAllPrimaryForCountry(countryId)
                .replaceWithVoid();
    }

    @Override
    public Uni<CountryCurrency> updateOperationalStatus(Integer countryId, Integer currencyId, Boolean isOperational) {
        return repository.findByCountryIdAndCurrencyId(countryId, currencyId)
                .onItem().transformToUni(entity -> {
                    if (entity == null) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Country-Currency relationship not found")
                        );
                    }
                    entity.setIsOperational(isOperational);
                    return repository.persist(entity)
                            .call(() -> repository.flush())  // Forzar flush para escribir en DB inmediatamente
                            .onItem().transform(mapper::toDomain);
                });
    }

    @Override
    public Uni<CountryCurrency> update(CountryCurrency countryCurrency) {
        return repository.findById(countryCurrency.getId())
                .onItem().transformToUni(existingEntity -> {
                    if (existingEntity == null) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("CountryCurrency not found with id: " + countryCurrency.getId())
                        );
                    }

                    existingEntity.setIsPrimary(countryCurrency.getIsPrimary());
                    existingEntity.setIsOperational(countryCurrency.getIsOperational());
                    existingEntity.setEffectiveDate(countryCurrency.getEffectiveDate());

                    return repository.persist(existingEntity)
                            .call(() -> repository.flush())  // Forzar flush para escribir en DB inmediatamente
                            .onItem().transform(mapper::toDomain);
                });
    }
}