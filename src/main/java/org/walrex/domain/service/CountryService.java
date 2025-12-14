package org.walrex.domain.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.CountryFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.AvailabilityResponse;
import org.walrex.application.dto.response.CountryResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.input.*;
import org.walrex.application.port.output.CountryCachePort;
import org.walrex.application.port.output.CountryQueryPort;
import org.walrex.application.port.output.CountryRepositoryPort;
import org.walrex.domain.exception.CountryNotFoundException;
import org.walrex.domain.exception.DuplicateCountryException;
import org.walrex.domain.model.Country;
import org.walrex.infrastructure.adapter.inbound.mapper.CountryDtoMapper;
import org.walrex.infrastructure.adapter.outbound.cache.CountryCacheKeyGenerator;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.CountryCache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Transactional
@ApplicationScoped
public class CountryService implements
        CreateCountryUseCase,
        ListCountryUseCase,
        GetCountryUseCase,
        UpdateCountryUseCase,
        DeleteCountryUseCase,
        CheckAvailabilityCountryUseCase
{

    @Inject
    CountryRepositoryPort countryRepositoryPort;

    @Inject
    CountryQueryPort countryQueryPort;

    @Inject
    @CountryCache
    CountryCachePort countryCachePort;

    @Inject
    CountryDtoMapper countryDtoMapper;

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    /**
     * Crea un nuevo pais.
     *
     */
    @Override
    public Uni<Country> agregar(Country country) {
        log.info("Creating currency: {} ({})", country.getName(), country.getAlphabeticCode3());
        // Validar unicidad de cada campo
        return validateUniqueness(country.getAlphabeticCode3(), country.getNumericCode(), country.getName(), null)
                .onItem().transformToUni(v -> countryRepositoryPort.save(country))
                .call(savedCurrency -> {
                    // Invalidar cache después de crear
                    log.debug("Invalidating currency cache after creation");
                    return countryCachePort.invalidateAll();
                });
    }

    /**
     * Lista paises con paginación y filtros.
     *
     * Implementa cache-aside pattern:
     * 1. Intenta obtener del cache
     * 2. Si no existe, consulta la DB
     * 3. Cachea el resultado
     * 4. Devuelve el resultado
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales (search, alphabeticCode, etc.)
     * @return Uni con respuesta paginada
     */
    @Override
    public Uni<PagedResponse<CountryResponse>> listar(PageRequest pageRequest, CountryFilter filter) {
        log.info("Listing countries with page: {}, size: {}, filter: {}",
                pageRequest.getPage(), pageRequest.getSize(), filter);

        // Generar clave única de cache
        String cacheKey = CountryCacheKeyGenerator.generateKey(pageRequest, filter);

        // Cache-aside pattern
        return countryCachePort.get(cacheKey)
                .onItem().transformToUni(cachedResult -> {
                    if (cachedResult != null) {
                        log.debug("Returning cached result for key: {}", cacheKey);
                        return Uni.createFrom().item(cachedResult);
                    }

                    // Cache miss - consultar DB
                    log.debug("Cache miss for key: {}. Querying database.", cacheKey);
                    return fetchFromDatabaseAndCache(pageRequest, filter, cacheKey);
                });
    }

    @Override
    public Uni<Country> execute(Integer id, Country country) {
        log.info("Updating currency id: {}", id);

        // Validar unicidad excluyendo el ID actual
        return validateUniqueness(
                country.getAlphabeticCode3(),
                country.getNumericCode(),
                country.getName(),
                id
        ).onItem().transformToUni(v -> {
                    country.setId(id);
                    return countryRepositoryPort.update(country);
                })
                .call(updatedCurrency -> {
                    // Invalidar cache después de actualizar
                    log.debug("Invalidating currency cache after update");
                    return countryCachePort.invalidateAll();
                });
    }

    /**
     * Consulta la DB y cachea el resultado.
     */
    private Uni<PagedResponse<CountryResponse>> fetchFromDatabaseAndCache(
            PageRequest pageRequest,
            CountryFilter filter,
            String cacheKey) {

        return countryQueryPort.findAll(pageRequest, filter)
                .onItem().transform(pagedResult -> {
                    var responses = pagedResult.content().stream()
                            .map(countryDtoMapper::toResponse)
                            .toList();

                    // Convert page from 0-based (backend) to 1-based (frontend)
                    return PagedResponse.of(
                            responses,
                            pagedResult.page() + 1,
                            pagedResult.size(),
                            pagedResult.totalElements()
                    );
                })
                .call(result -> {
                    // Cachear el resultado (fire-and-forget)
                    log.debug("Caching result for key: {}", cacheKey);
                    return countryCachePort.put(cacheKey, result, CACHE_TTL);
                });
    }

    /**
     * Obtiene todas los paises activas como un stream reactivo.
     *
     * @return Multi que emite cada pais individualmente
     */
    @Override
    public Multi<CountryResponse> streamAll() {
        log.info("Streaming all active countries");
        return countryQueryPort.streamAll()
                .onItem().transform(countryDtoMapper::toResponse);
    }

    /**
     * Obtiene todos los paises activos como un stream con filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada pais que cumple los filtros
     */
    @Override
    public Multi<CountryResponse> streamWithFilter(CountryFilter filter) {
        log.info("Streaming countries with filter: {}", filter);
        return countryQueryPort.streamWithFilter(filter)
                .onItem().transform(countryDtoMapper::toResponse);
    }

    /**
     * Obtiene una moneda por su ID.
     *
     * @throws CountryNotFoundException si no existe una moneda con el ID proporcionado
     */
    @Override
    public Uni<Country> findById(Integer id) {
        log.info("Getting currency by id: {}", id);
        return countryQueryPort.findById(id)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new CountryNotFoundException(id)
                ));
    }

    /**
     * Obtiene un pais por su código alfabético ISO 3166.
     *
     * @throws CountryNotFoundException si no existe un pais con el código proporcionado
     */
    @Override
    public Uni<Country> findByAlphabeticCode3(String alphabeticCode) {
        log.info("Getting country by alphabetic code: {}", alphabeticCode);
        return countryQueryPort.findByAlphabeticCode(alphabeticCode)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new CountryNotFoundException("Country not found with alphabetic code: " + alphabeticCode)
                ));
    }


    /**
     * Valida que los campos únicos no existan en otros registros.
     */
    private Uni<Void> validateUniqueness(
            String alphabeticCode,
            Integer numericCode,
            String name,
            Integer excludeId
    ) {
        return countryQueryPort.existsByAlphabeticCode(alphabeticCode, excludeId)
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateCountryException("alphabeticCode", alphabeticCode));
                    }
                    return countryQueryPort.existsByNumericCode(numericCode, excludeId);
                })
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateCountryException("numericCode", numericCode.toString()));
                    }
                    return countryQueryPort.existsByName(name, excludeId);
                })
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateCountryException("name", name));
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Obtiene un país por su código numérico ISO 3166.
     *
     * @throws CountryNotFoundException si no existe un pais con el código proporcionado
     */
    @Override
    public Uni<Country> findByNumericCode(String numericCode) {
        log.info("Getting country by numeric code: {}", numericCode);
        return countryQueryPort.findByNumericCode(numericCode)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new CountryNotFoundException("Country not found with numeric code: " + numericCode)
                ));
    }


    @Override
    public Uni<AvailabilityResponse> checkAlphabeticCode(String alphabeticCode, Integer excludeId) {
        log.info("Checking alphabetic code availability: {}", alphabeticCode);
        return countryQueryPort.existsByAlphabeticCode(alphabeticCode, excludeId)
                .onItem().transform(exists -> AvailabilityResponse.of("alphabeticCode", alphabeticCode, !exists));
    }

    @Override
    public Uni<AvailabilityResponse> checkNumericCode(Integer numericCode, Integer excludeId) {
        log.info("Checking numeric code availability: {}", numericCode);
        return countryQueryPort.existsByNumericCode(numericCode, excludeId)
                .onItem().transform(exists -> AvailabilityResponse.of("numericCode", numericCode.toString().trim(), !exists));
    }

    /**
     * Verifica si un nombre está disponible.
     */
    @Override
    public Uni<AvailabilityResponse> checkName(String name, Integer excludeId) {
        log.info("Checking name availability: {}", name);
        return countryQueryPort.existsByName(name, excludeId)
                .onItem().transform(exists -> AvailabilityResponse.of("name", name, !exists));
    }

    /**
     * Verifica disponibilidad de múltiples campos a la vez.
     */
    @Override
    public Uni<List<AvailabilityResponse>> checkAll(
            String alphabeticCode,
            String numericCode,
            String name,
            Integer excludeId) {
        log.info("Checking availability for all fields");

        var checks = new ArrayList<Uni<AvailabilityResponse>>();

        if (alphabeticCode != null && !alphabeticCode.isBlank()) {
            checks.add(checkAlphabeticCode(alphabeticCode, excludeId));
        }
        if (numericCode != null && !numericCode.isBlank()) {
            checks.add(checkNumericCode(Integer.parseInt(numericCode), excludeId));
        }
        if (name != null && !name.isBlank()) {
            checks.add(checkName(name, excludeId));
        }

        return Uni.combine().all().unis(checks).with(list ->
                list.stream()
                        .map(obj -> (AvailabilityResponse) obj)
                        .toList()
        );
    }

    @Override
    public Uni<Boolean> deshabilitar(Integer id) {
        log.info("Soft deleting country id: {}", id);
        return countryRepositoryPort.softDelete(id)
                .call(deleted -> {
                    if (deleted) {
                        // Invalidar cache después de eliminar
                        log.debug("Invalidating country cache after deletion");
                        return countryCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    @Override
    public Uni<Boolean> habilitar(Integer id) {
        log.info("Restoring country id: {}", id);
        return countryRepositoryPort.restore(id)
                .call(restored -> {
                    if (restored) {
                        // Invalidar cache después de restaurar
                        log.debug("Invalidating country cache after restoration");
                        return countryCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}