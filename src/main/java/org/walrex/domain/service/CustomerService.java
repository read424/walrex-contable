package org.walrex.domain.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.CustomerFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.CustomerResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.input.CreateCustomerUseCase;
import org.walrex.application.port.input.GetCustomerUseCase;
import org.walrex.application.port.input.ListCustomersUseCase;
import org.walrex.application.port.input.UpdateCustomerUseCase;
import org.walrex.application.port.output.CustomerCachePort;
import org.walrex.application.port.output.CustomerQueryPort;
import org.walrex.application.port.output.CustomerRepositoryPort;
import org.walrex.domain.model.Customer;
import org.walrex.infrastructure.adapter.inbound.mapper.CustomerDtoMapper;
import org.walrex.infrastructure.adapter.logging.LogExecutionTime;
import org.walrex.infrastructure.adapter.outbound.cache.CustomerCacheKeyGenerator;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.CustomerCache;

/**
 * Servicio de dominio que implementa los casos de uso de Customer.
 *
 * Siguiendo el patrón hexagonal, este servicio:
 * - Implementa las interfaces de puerto de entrada (use cases)
 * - Orquesta la lógica de negocio
 * - Delega operaciones de persistencia a los puertos de salida
 * - Maneja validaciones de negocio
 */
@Slf4j
@Transactional
@ApplicationScoped
public class CustomerService implements
                CreateCustomerUseCase,
                UpdateCustomerUseCase,
                ListCustomersUseCase,
                GetCustomerUseCase {

        @Inject
        CustomerRepositoryPort customerRepositoryPort;

        @Inject
        CustomerQueryPort customerQueryPort;

        @Inject
        @CustomerCache
        CustomerCachePort customerCachePort;

        @Inject
        CustomerDtoMapper customerDtoMapper;

        private static final java.time.Duration CACHE_TTL = java.time.Duration.ofMinutes(5);

        // ==================== CreateCustomerUseCase ====================

        /**
         * Crea un nuevo cliente.
         *
         * @param customer Datos del cliente a crear
         * @return Uni con el cliente creado
         */
        @Override
        @WithSpan("CustomerService.create")
        @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true, logReturn = true)
        public Uni<Customer> agregar(Customer customer) {
                log.info("Creating customer: {} {} ({})",
                                customer.getFirstName(),
                                customer.getLastName(),
                                customer.getNumberDocument());

                // Validar unicidad del documento y email
                return validateUniqueness(
                            customer.getIdTypeDocument(),
                            customer.getNumberDocument(),
                            customer.getEmail(),
                            null
                        ).onItem()
                        .transformToUni(v -> customerRepositoryPort.save(customer))
                        .call(savedCustomer -> {
                            // Invalidar cache después de crear
                            log.debug("Invalidating customer cache after creation");
                            return customerCachePort.invalidateAll();
                        });
        }

        // ==================== UpdateCustomerUseCase ====================

        /**
         * Actualiza un cliente existente con nuevos datos.
         *
         * @param id       Identificador del cliente a actualizar
         * @param customer Nuevos datos para el cliente
         * @return Uni con el cliente actualizado
         */
        @Override
        @WithSpan("CustomerService.update")
        @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true, logReturn = true)
        public Uni<Customer> actualizar(Integer id, Customer customer) {
                log.info("Updating customer id: {}", id);

                // Validar unicidad excluyendo el ID actual
                return validateUniqueness(
                                customer.getIdTypeDocument(),
                                customer.getNumberDocument(),
                                customer.getEmail(),
                                id).onItem().transformToUni(v -> {
                                        // Asegurar que el ID esté establecido
                                        customer.setId(id);
                                        return customerRepositoryPort.update(customer);
                                })
                                .call(updatedCustomer -> {
                                        // Invalidar cache después de actualizar
                                        log.debug("Invalidating customer cache after update");
                                        return customerCachePort.invalidateAll();
                                });
        }

        // ==================== ListCustomersUseCase ====================

        /**
         * Lista clientes con paginación y filtros.
         *
         * Implementa cache-aside pattern:
         * 1. Intenta obtener del cache
         * 2. Si no existe, consulta la DB
         * 3. Cachea el resultado
         * 4. Devuelve el resultado
         *
         * @param pageRequest Configuración de paginación (page, size, sort)
         * @param filter      Filtros opcionales (search, idTypeDocument,
         *                    numberDocument, etc.)
         * @return Uni con respuesta paginada
         */
        @Override
        @WithSpan("CustomerService.list")
        @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = true, logReturn = false)
        public Uni<PagedResponse<CustomerResponse>> listar(PageRequest pageRequest, CustomerFilter filter) {
                log.info("Listing customers with page: {}, size: {}, filter: {}",
                                pageRequest.getPage(), pageRequest.getSize(), filter);

                // Generar clave única de cache
                String cacheKey = CustomerCacheKeyGenerator.generateKey(pageRequest, filter);

                // Cache-aside pattern
                return customerCachePort.get(cacheKey)
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
        private Uni<PagedResponse<CustomerResponse>> fetchFromDatabaseAndCache(
                        PageRequest pageRequest,
                        CustomerFilter filter,
                        String cacheKey) {

                return customerQueryPort.findAll(pageRequest, filter)
                                .onItem().transform(pagedResult -> {
                                        var responses = pagedResult.content().stream()
                                                        .map(customerDtoMapper::toResponse)
                                                        .toList();

                                        // Convert page from 0-based (backend) to 1-based (frontend)
                                        return PagedResponse.of(
                                                        responses,
                                                        pagedResult.page() + 1,
                                                        pagedResult.size(),
                                                        pagedResult.totalElements());
                                })
                                .call(result -> {
                                        // Cachear el resultado (fire-and-forget)
                                        log.debug("Caching result for key: {}", cacheKey);
                                        return customerCachePort.put(cacheKey, result, CACHE_TTL);
                                });
        }

        /**
         * Obtiene todos los clientes activos como un stream reactivo.
         *
         * @return Multi que emite cada cliente individualmente
         */
        @Override
        public Multi<CustomerResponse> streamAll() {
                log.info("Streaming all active customers");
                return customerQueryPort.streamAll()
                                .onItem().transform(customerDtoMapper::toResponse);
        }

        /**
         * Obtiene todos los clientes activos como un stream con filtros.
         *
         * @param filter Filtros a aplicar
         * @return Multi que emite cada cliente que cumple los filtros
         */
        @Override
        public Multi<CustomerResponse> streamWithFilter(CustomerFilter filter) {
                log.info("Streaming customers with filter: {}", filter);
                return customerQueryPort.streamWithFilter(filter)
                                .onItem().transform(customerDtoMapper::toResponse);
        }

        // ==================== GetCustomerUseCase ====================

        /**
         * Obtiene un cliente por su ID.
         *
         * @param id Identificador del cliente
         * @return Uni con el cliente encontrado
         */
        @Override
        public Uni<Customer> findById(Integer id) {
                log.info("Getting customer by id: {}", id);
                return customerQueryPort.findById(id)
                                .onItem().transformToUni(optionalCustomer -> {
                                        if (optionalCustomer.isEmpty()) {
                                                return Uni.createFrom().failure(
                                                                new org.walrex.domain.exception.CustomerNotFoundException(
                                                                                id));
                                        }
                                        return Uni.createFrom().item(optionalCustomer.get());
                                });
        }

        // ==================== Validaciones Privadas ====================

        /**
         * Valida que el documento y email no existan en otros registros.
         *
         * @param idTypeDocument Tipo de documento
         * @param numberDocument Número de documento
         * @param email          Email del cliente
         * @param excludeId      ID a excluir de la búsqueda (null para no excluir)
         * @return Uni<Void> que falla si hay duplicados
         */
        private Uni<Void> validateUniqueness(
                        Integer idTypeDocument,
                        String numberDocument,
                        String email,
                        Integer excludeId) {
                return customerQueryPort.existsByDocument(idTypeDocument, numberDocument, excludeId)
                                .onItem().transformToUni(exists -> {
                                        if (exists) {
                                                return Uni.createFrom().failure(
                                                                new IllegalArgumentException(
                                                                                "Ya existe un cliente con el documento: "
                                                                                                + numberDocument));
                                        }
                                        return customerQueryPort.existsByEmail(email, excludeId);
                                })
                                .onItem().transformToUni(exists -> {
                                        if (exists) {
                                                return Uni.createFrom().failure(
                                                                new IllegalArgumentException(
                                                                                "Ya existe un cliente con el email: "
                                                                                                + email));
                                        }
                                        return Uni.createFrom().voidItem();
                                });
        }
}
