package org.walrex.domain.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.CurrencyFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.AvailabilityResponse;
import org.walrex.application.dto.response.CurrencyResponse;
import org.walrex.application.dto.response.CurrencySelectResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.input.CheckAvailabilityUseCase;
import org.walrex.application.port.input.CreateCurrencyUseCase;
import org.walrex.application.port.input.DeleteCurrencyUseCase;
import org.walrex.application.port.input.GetCurrencyUseCase;
import org.walrex.application.port.input.ListCurrenciesUseCase;
import org.walrex.application.port.input.UpdateCurrencyUseCase;
import org.walrex.application.port.output.CurrencyCachePort;
import org.walrex.application.port.output.CurrencyQueryPort;
import org.walrex.application.port.output.CurrencyRepositoryPort;
import org.walrex.domain.exception.CurrencyNotFoundException;
import org.walrex.domain.exception.DuplicateCurrencyException;
import org.walrex.domain.model.Currency;
import org.walrex.infrastructure.adapter.inbound.mapper.CurrencyDtoMapper;
import org.walrex.infrastructure.adapter.outbound.cache.CurrencyCacheKeyGenerator;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.CurrencyCache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Transactional
@ApplicationScoped
public class CurrencyService implements
        CreateCurrencyUseCase,
        ListCurrenciesUseCase,
        GetCurrencyUseCase,
        UpdateCurrencyUseCase,
        DeleteCurrencyUseCase,
        CheckAvailabilityUseCase {

    @Inject
    CurrencyRepositoryPort currencyRepositoryPort;

    @Inject
    CurrencyQueryPort currencyQueryPort;

    @Inject
    @CurrencyCache
    CurrencyCachePort currencyCachePort;

    @Inject
    CurrencyDtoMapper currencyDtoMapper;

    // TTL del cache: 5 minutos para listado paginado
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    // TTL del cache: 15 minutos para listado completo (/all endpoint)
    // Mayor TTL porque los datos son más estáticos y se invalida en cada cambio
    private static final Duration CACHE_ALL_TTL = Duration.ofMinutes(15);

    /**
     * Crea una nueva moneda.
     *
     * @throws DuplicateCurrencyException si algún campo único ya existe
     */
    @Override
    public Uni<Currency> execute(Currency currency) {
        log.info("Creating currency: {} ({})", currency.getName(), currency.getAlphabeticCode());

        // Validar unicidad de cada campo
        return validateUniqueness(currency.getAlphabeticCode(), currency.getNumericCode(), currency.getName(), null)
                .onItem().transformToUni(v -> currencyRepositoryPort.save(currency))
                .call(savedCurrency -> {
                    // Invalidar cache después de crear
                    log.debug("Invalidating currency cache after creation");
                    return currencyCachePort.invalidateAll();
                });
    }

    /**
     * Lista monedas con paginación y filtros.
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
    public Uni<PagedResponse<CurrencyResponse>> execute(PageRequest pageRequest, CurrencyFilter filter) {
        log.info("Listing currencies with page: {}, size: {}, filter: {}",
                pageRequest.getPage(), pageRequest.getSize(), filter);

        // Generar clave única de cache
        String cacheKey = CurrencyCacheKeyGenerator.generateKey(pageRequest, filter);

        // Cache-aside pattern
        return currencyCachePort.get(cacheKey)
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

    /**
     * Consulta la DB y cachea el resultado.
     */
    private Uni<PagedResponse<CurrencyResponse>> fetchFromDatabaseAndCache(
            PageRequest pageRequest,
            CurrencyFilter filter,
            String cacheKey) {

        return currencyQueryPort.findAll(pageRequest, filter)
                .onItem().transform(pagedResult -> {
                    var responses = pagedResult.content().stream()
                            .map(currencyDtoMapper::toResponse)
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
                    return currencyCachePort.put(cacheKey, result, CACHE_TTL);
                });
    }

    /**
     * Obtiene todas las monedas que cumplen el filtro sin paginación.
     * Retorna un DTO optimizado para componentes de selección.
     *
     * Implementa cache-aside pattern con TTL de 15 minutos:
     * 1. Intenta obtener del cache
     * 2. Si no existe, consulta la DB
     * 3. Cachea el resultado por 15 minutos
     * 4. Devuelve el resultado
     *
     * El cache se invalida automáticamente en create/update/delete.
     *
     * @param filter Filtros opcionales (por defecto solo monedas activas)
     * @return Uni con lista completa de monedas optimizadas
     */
    @Override
    public Uni<List<CurrencySelectResponse>> findAll(CurrencyFilter filter) {
        log.info("Listing all currencies with filter: {}", filter);

        // Generar clave única de cache
        String cacheKey = CurrencyCacheKeyGenerator.generateKey(filter);

        // Cache-aside pattern
        return currencyCachePort.<CurrencySelectResponse>getList(cacheKey)
                .onItem().transformToUni(cachedResult -> {
                    if (cachedResult != null) {
                        log.debug("Returning cached result for key: {}", cacheKey);
                        return Uni.createFrom().item(cachedResult);
                    }

                    // Cache miss - consultar DB
                    log.debug("Cache miss for key: {}. Querying database.", cacheKey);
                    return fetchAllAndCache(filter, cacheKey);
                });
    }

    /**
     * Consulta la DB y cachea el resultado para endpoint /all.
     */
    private Uni<List<CurrencySelectResponse>> fetchAllAndCache(
            CurrencyFilter filter,
            String cacheKey) {

        return currencyQueryPort.findAllWithFilter(filter)
                .onItem().transform(currencyDtoMapper::toSelectResponseList)
                .call(result -> {
                    // Cachear el resultado por 15 minutos (fire-and-forget)
                    log.debug("Caching result for key: {} with TTL of {} minutes",
                            cacheKey, CACHE_ALL_TTL.toMinutes());
                    return currencyCachePort.putList(cacheKey, result, CACHE_ALL_TTL);
                });
    }

    /**
     * Obtiene todas las monedas activas como un stream reactivo.
     *
     * @return Multi que emite cada moneda individualmente
     */
    @Override
    public Multi<CurrencyResponse> streamAll() {
        log.info("Streaming all active currencies");
        return currencyQueryPort.streamAll()
                .onItem().transform(currencyDtoMapper::toResponse);
    }

    /**
     * Obtiene todas las monedas activas como un stream con filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada moneda que cumple los filtros
     */
    @Override
    public Multi<CurrencyResponse> streamWithFilter(CurrencyFilter filter) {
        log.info("Streaming currencies with filter: {}", filter);
        return currencyQueryPort.streamWithFilter(filter)
                .onItem().transform(currencyDtoMapper::toResponse);
    }

    // ==================== GetCurrencyUseCase ====================

    /**
     * Obtiene una moneda por su ID.
     *
     * @throws CurrencyNotFoundException si no existe una moneda con el ID proporcionado
     */
    @Override
    public Uni<Currency> findById(Integer id) {
        log.info("Getting currency by id: {}", id);
        return currencyQueryPort.findById(id)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new CurrencyNotFoundException(id)
                ));
    }

    /**
     * Obtiene una moneda por su código alfabético ISO 4217.
     *
     * @throws CurrencyNotFoundException si no existe una moneda con el código proporcionado
     */
    @Override
    public Uni<Currency> findByAlphabeticCode(String alphabeticCode) {
        log.info("Getting currency by alphabetic code: {}", alphabeticCode);
        return currencyQueryPort.findByAlphabeticCode(alphabeticCode)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new CurrencyNotFoundException("Currency not found with alphabetic code: " + alphabeticCode)
                ));
    }

    /**
     * Obtiene una moneda por su código numérico ISO 4217.
     *
     * @throws CurrencyNotFoundException si no existe una moneda con el código proporcionado
     */
    @Override
    public Uni<Currency> findByNumericCode(String numericCode) {
        log.info("Getting currency by numeric code: {}", numericCode);
        return currencyQueryPort.findByNumericCode(numericCode)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new CurrencyNotFoundException("Currency not found with numeric code: " + numericCode)
                ));
    }

    // ==================== UpdateCurrencyUseCase ====================

    /**
     * Actualiza una moneda existente con nuevos datos.
     */
    @Override
    public Uni<Currency> execute(Integer id, Currency currency) {
        log.info("Updating currency id: {}", id);

        // Validar unicidad excluyendo el ID actual
        return validateUniqueness(
                currency.getAlphabeticCode(),
                currency.getNumericCode(),
                currency.getName(),
                id
        ).onItem().transformToUni(v -> currencyRepositoryPort.update(currency))
                .call(updatedCurrency -> {
                    // Invalidar cache después de actualizar
                    log.debug("Invalidating currency cache after update");
                    return currencyCachePort.invalidateAll();
                });
    }

    // ==================== DeleteCurrencyUseCase ====================

    /**
     * Elimina lógicamente una moneda (soft delete).
     */
    @Override
    public Uni<Boolean> execute(Integer id) {
        log.info("Soft deleting currency id: {}", id);
        return currencyRepositoryPort.softDelete(id)
                .call(deleted -> {
                    if (deleted) {
                        // Invalidar cache después de eliminar
                        log.debug("Invalidating currency cache after deletion");
                        return currencyCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Restaura una moneda previamente eliminada.
     */
    @Override
    public Uni<Boolean> restore(Integer id) {
        log.info("Restoring currency id: {}", id);
        return currencyRepositoryPort.restore(id)
                .call(restored -> {
                    if (restored) {
                        // Invalidar cache después de restaurar
                        log.debug("Invalidating currency cache after restoration");
                        return currencyCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    // ==================== CheckAvailabilityUseCase ====================

    /**
     * Verifica si un código alfabético está disponible.
     */
    @Override
    public Uni<AvailabilityResponse> checkAlphabeticCode(String alphabeticCode, Integer excludeId) {
        log.info("Checking alphabetic code availability: {}", alphabeticCode);
        return currencyQueryPort.existsByAlphabeticCode(alphabeticCode, excludeId)
                .onItem().transform(exists -> AvailabilityResponse.of("alphabeticCode", alphabeticCode, !exists));
    }

    /**
     * Verifica si un código numérico está disponible.
     */
    @Override
    public Uni<AvailabilityResponse> checkNumericCode(String numericCode, Integer excludeId) {
        log.info("Checking numeric code availability: {}", numericCode);
        return currencyQueryPort.existsByNumericCode(Integer.parseInt(numericCode), excludeId)
                .onItem().transform(exists -> AvailabilityResponse.of("numericCode", numericCode, !exists));
    }

    /**
     * Verifica si un nombre está disponible.
     */
    @Override
    public Uni<AvailabilityResponse> checkName(String name, Integer excludeId) {
        log.info("Checking name availability: {}", name);
        return currencyQueryPort.existsByName(name, excludeId)
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
            Integer excludeId
    ) {
        log.info("Checking availability for all fields");

        var checks = new ArrayList<Uni<AvailabilityResponse>>();

        if (alphabeticCode != null && !alphabeticCode.isBlank()) {
            checks.add(checkAlphabeticCode(alphabeticCode, excludeId));
        }
        if (numericCode != null && !numericCode.isBlank()) {
            checks.add(checkNumericCode(numericCode, excludeId));
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

    /**
     * Valida que los campos únicos no existan en otros registros.
     */
    private Uni<Void> validateUniqueness(
            String alphabeticCode,
            Integer numericCode,
            String name,
            Integer excludeId
    ) {
        return currencyQueryPort.existsByAlphabeticCode(alphabeticCode, excludeId)
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateCurrencyException("alphabeticCode", alphabeticCode));
                    }
                    return currencyQueryPort.existsByNumericCode(numericCode, excludeId);
                })
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateCurrencyException("numericCode", numericCode.toString()));
                    }
                    return currencyQueryPort.existsByName(name, excludeId);
                })
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateCurrencyException("name", name));
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Valida y normaliza el campo de ordenamiento.
     * Previene SQL injection permitiendo solo campos conocidos.
     */
    private String validateSortField(String field) {
        return switch (field == null ? "" : field.toLowerCase()) {
            case "alphabeticcode", "alphabetic_code" -> "alphabeticCode";
            case "numericcode", "numeric_code" -> "numericCode";
            case "createdat", "created_at" -> "createdAt";
            case "updatedat", "updated_at" -> "updatedAt";
            case "active" -> "active";
            default -> "name"; // Default seguro
        };
    }
}