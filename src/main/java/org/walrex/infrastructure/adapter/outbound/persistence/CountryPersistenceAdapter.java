package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.query.CountryFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.port.output.CountryQueryPort;
import org.walrex.application.port.output.CountryRepositoryPort;
import org.walrex.domain.model.Country;
import org.walrex.domain.model.PagedResult;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.CountryMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.CountryRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia que implementa los puertos de salida para Country.
 *
 * Siguiendo el patrón hexagonal (Ports & Adapters), este adaptador:
 * - Implementa las interfaces de puerto (CountryRepositoryPort, CountryQueryPort)
 * - Traduce entre el modelo de dominio (Country) y la capa de persistencia (CountryEntity)
 * - Utiliza el mapper para transformaciones
 * - Delega operaciones de persistencia al repository de Panache
 */
@ApplicationScoped
public class CountryPersistenceAdapter implements CountryRepositoryPort, CountryQueryPort {

    @Inject
    CountryRepository countryRepository;

    @Inject
    CountryMapper countryMapper;

    @Override
    public Uni<Country> save(Country country) {

        return countryRepository.persist(countryMapper.toEntity(country))
                .onItem().transform(countryMapper::toDomain);
    }

    @Override
    public Uni<Country> update(Country country) {
        return countryRepository.findById(country.getId())
                .onItem().transformToUni(existingEntity -> {
                    if (existingEntity == null) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Country not found with id: " + country.getId())
                        );
                    }

                    // Actualizar solo los campos modificables
                    existingEntity.setAlphabeticCode2(country.getAlphabeticCode2());
                    existingEntity.setAlphabeticCode3(country.getAlphabeticCode3());
                    existingEntity.setNumericCode(country.getNumericCode());
                    existingEntity.setName(country.getName());
                    existingEntity.setPhoneCode(country.getPhoneCode());
                    existingEntity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

                    // persist() actualizará la entidad existente porque ya está managed
                    return countryRepository.persist(existingEntity)
                            .onItem().transform(countryMapper::toDomain);
                });
    }

    @Override
    public Uni<Boolean> softDelete(Integer id) {
        return countryRepository.findById(id)
                .onItem().transformToUni(entity -> {
                    if (entity == null || entity.getDeletedAt() != null) {
                        return Uni.createFrom().item(false);
                    }
                    entity.setDeletedAt(OffsetDateTime.now());
                    entity.setStatus("1");
                    entity.setUpdatedAt(OffsetDateTime.now());
                    return countryRepository.persist(entity)
                            .call(() -> countryRepository.flush())  // Forzar flush para asegurar que se escribió en DB
                            .onItem().transform(e -> true);
                });
    }

    @Override
    public Uni<Boolean> hardDelete(Integer id) {
        return countryRepository.deleteById(id);
    }

    @Override
    public Uni<Boolean> restore(Integer id) {
        return countryRepository.findById(id)
                .onItem().transformToUni(entity -> {
                    if (entity == null || entity.getDeletedAt() == null) {
                        return Uni.createFrom().item(false);
                    }
                    entity.setDeletedAt(null);
                    entity.setStatus("1");
                    entity.setUpdatedAt(OffsetDateTime.now());
                    return countryRepository.persist(entity)
                            .call(() -> countryRepository.flush())  // Forzar flush para asegurar que se escribió en DB
                            .onItem().transform(e -> true);
                });
    }

    @Override
    public Uni<Optional<Country>> findById(Integer id) {
        return countryRepository.findActiveById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(countryMapper::toDomain));
    }

    @Override
    public Uni<Optional<Country>> findByIdIncludingDeleted(Integer id) {
        return countryRepository.findById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(countryMapper::toDomain));
    }

    @Override
    public Uni<Optional<Country>> findByAlphabeticCode(String alphabeticCode) {
        return countryRepository.findByAlphabeticCode(alphabeticCode)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(countryMapper::toDomain));
    }

    @Override
    public Uni<Optional<Country>> findByNumericCode(String numericCode) {
        return countryRepository.findByNumericCode(numericCode)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(countryMapper::toDomain));
    }

    @Override
    public Uni<Optional<Country>> findByName(String name) {
        return countryRepository.findByName(name)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(countryMapper::toDomain));
    }

    // ==================== CurrencyQueryPort - Verificaciones de Existencia ====================

    @Override
    public Uni<Boolean> existsByAlphabeticCode(String alphabeticCode, Integer excludeId) {
        return countryRepository.existsByAlphabeticCode(alphabeticCode, excludeId);
    }

    @Override
    public Uni<Boolean> existsByNumericCode(Integer numericCode, Integer excludeId) {
        return countryRepository.existsByNumericCode(numericCode, excludeId);
    }

    @Override
    public Uni<Boolean> existsByName(String name, Integer excludeId) {
        return countryRepository.existsByName(name, excludeId);
    }

    @Override
    public Uni<PagedResult<Country>> findAll(PageRequest pageRequest, CountryFilter filter) {
        // Ejecutar ambas queries en paralelo para mejor performance
        Uni<List<Country>> dataUni = countryRepository.findWithFilters(pageRequest, filter)
                .onItem().transform(countryMapper::toDomain)
                .collect().asList();

        Uni<Long> countUni = countryRepository.countWithFilters(filter);

        // Combinar ambos resultados en un PagedResult
        return Uni.combine().all().unis(dataUni, countUni)
                .asTuple()
                .onItem().transform(tuple -> PagedResult.of(
                        tuple.getItem1(),              // List<Currency>
                        pageRequest.getPage(),         // page
                        pageRequest.getSize(),         // size
                        tuple.getItem2()               // totalElements
                ));
    }

    @Override
    public Uni<Long> count(CountryFilter filter) {
        return countryRepository.countWithFilters(filter);
    }

    // ==================== CurrencyQueryPort - Streaming ====================

    @Override
    public Multi<Country> streamAll() {
        return countryRepository.streamAll()
                .onItem().transform(countryMapper::toDomain);
    }

    @Override
    public Multi<Country> streamWithFilter(CountryFilter filter) {
        // Usamos findAll con tamaño máximo y convertimos el resultado a Multi
        return findAll(PageRequest.builder().page(0).size(Integer.MAX_VALUE).build(), filter)
                .onItem().transformToMulti(pagedResult -> Multi.createFrom().iterable(pagedResult.content()));
    }

    // ==================== CurrencyQueryPort - Consultas Especiales ====================

    @Override
    public Uni<List<Country>> findAllDeleted() {
        return countryRepository.findAllDeleted()
                .onItem().transform(entities -> entities.stream()
                        .map(countryMapper::toDomain)
                        .toList());
    }

    @Override
    public Uni<List<Country>> findByAlphabeticCodes(List<String> codes) {
        // Hacemos múltiples búsquedas y las combinamos
        List<Uni<Optional<Country>>> searches = codes.stream()
                .map(this::findByAlphabeticCode)
                .toList();

        return Uni.combine().all().unis(searches)
                .with(results -> results.stream()
                        .filter(opt -> ((Optional<?>) opt).isPresent())
                        .map(opt -> ((Optional<Country>) opt).get())
                        .toList());
    }
}
