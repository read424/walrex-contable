package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.query.CustomerFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.port.output.CustomerQueryPort;
import org.walrex.application.port.output.CustomerRepositoryPort;
import org.walrex.domain.model.Customer;
import org.walrex.domain.model.PagedResult;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.CustomerMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.CustomerRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia que implementa los puertos de salida para Customer.
 *
 * Siguiendo el patrón hexagonal (Ports & Adapters), este adaptador:
 * - Implementa las interfaces de puerto (CustomerRepositoryPort,
 * CustomerQueryPort)
 * - Traduce entre el modelo de dominio (Customer) y la capa de persistencia
 * (CustomerEntity)
 * - Utiliza el mapper para transformaciones
 * - Delega operaciones de persistencia al repository de Panache
 */
@ApplicationScoped
public class CustomerPersistenceAdapter implements CustomerRepositoryPort, CustomerQueryPort {

    @Inject
    CustomerRepository repository;

    @Inject
    CustomerMapper mapper;

    // ==================== CustomerRepositoryPort - Escritura ====================

    @Override
    public Uni<Customer> save(Customer customer) {
        return repository.persist(mapper.toEntity(customer))
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Uni<Customer> update(Customer customer) {
        return repository.findById(customer.getId())
                .onItem().transformToUni(existingEntity -> {
                    if (existingEntity == null) {
                        return Uni.createFrom().failure(
                                new org.walrex.domain.exception.CustomerNotFoundException(customer.getId()));
                    }

                    // Actualizar campos usando mapper
                    mapper.updateEntityFromDomain(customer, existingEntity);
                    existingEntity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

                    return repository.persist(existingEntity)
                            .onItem().transform(mapper::toDomain);
                });
    }

    @Override
    public Uni<Boolean> softDelete(Integer id) {
        return repository.findById(id)
                .onItem().transformToUni(entity -> {
                    if (entity == null || entity.getDeletedAt() != null) {
                        return Uni.createFrom().item(false);
                    }
                    entity.setDeletedAt(OffsetDateTime.now());
                    entity.setUpdatedAt(OffsetDateTime.now());
                    return repository.persist(entity)
                            .call(() -> repository.flush()) // Forzar flush para asegurar que se escribió en DB
                            .onItem().transform(e -> true);
                });
    }

    @Override
    public Uni<Boolean> hardDelete(Integer id) {
        return repository.deleteById(id);
    }

    @Override
    public Uni<Boolean> restore(Integer id) {
        return repository.findById(id)
                .onItem().transformToUni(entity -> {
                    if (entity == null || entity.getDeletedAt() == null) {
                        return Uni.createFrom().item(false);
                    }
                    entity.setDeletedAt(null);
                    entity.setUpdatedAt(OffsetDateTime.now());
                    return repository.persist(entity)
                            .call(() -> repository.flush()) // Forzar flush para asegurar que se escribió en DB
                            .onItem().transform(e -> true);
                });
    }

    // ==================== CustomerQueryPort - Búsquedas ====================

    @Override
    public Uni<Optional<Customer>> findById(Integer id) {
        return repository.findActiveById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<Customer>> findByIdIncludingDeleted(Integer id) {
        return repository.findById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<Customer>> findByDocument(Integer idTypeDocument, String numberDocument) {
        return repository.findByDocument(idTypeDocument, numberDocument)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<Customer>> findByEmail(String email) {
        return repository.findByEmail(email)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    // ==================== CustomerQueryPort - Verificaciones de Existencia
    // ====================

    @Override
    public Uni<Boolean> existsByDocument(Integer idTypeDocument, String numberDocument, Integer excludeId) {
        return repository.existsByDocument(idTypeDocument, numberDocument, excludeId);
    }

    @Override
    public Uni<Boolean> existsByEmail(String email, Integer excludeId) {
        return repository.existsByEmail(email, excludeId);
    }

    // ==================== CustomerQueryPort - Paginación ====================

    @Override
    public Uni<PagedResult<Customer>> findAll(PageRequest pageRequest, CustomerFilter filter) {
        // Ejecutar ambas queries en paralelo para mejor performance
        Uni<List<Customer>> dataUni = repository.findWithFilters(pageRequest, filter)
                .onItem().transform(mapper::toDomain)
                .collect().asList();

        Uni<Long> countUni = repository.countWithFilters(filter);

        // Combinar ambos resultados en un PagedResult
        return Uni.combine().all().unis(dataUni, countUni)
                .asTuple()
                .onItem().transform(tuple -> PagedResult.of(
                        tuple.getItem1(), // List<Customer>
                        pageRequest.getPage(), // page
                        pageRequest.getSize(), // size
                        tuple.getItem2() // totalElements
                ));
    }

    @Override
    public Uni<Long> count(CustomerFilter filter) {
        return repository.countWithFilters(filter);
    }

    // ==================== CustomerQueryPort - Streaming ====================

    @Override
    public Multi<Customer> streamAll() {
        return repository.streamAll()
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Multi<Customer> streamWithFilter(CustomerFilter filter) {
        // Usamos findAll con tamaño máximo y convertimos el resultado a Multi
        return findAll(PageRequest.builder().page(0).size(Integer.MAX_VALUE).build(), filter)
                .onItem().transformToMulti(pagedResult -> Multi.createFrom().iterable(pagedResult.content()));
    }

    // ==================== CustomerQueryPort - Consultas Especiales
    // ====================

    @Override
    public Uni<List<Customer>> findAllDeleted() {
        return repository.findAllDeleted()
                .onItem().transform(entities -> entities.stream()
                        .map(mapper::toDomain)
                        .toList());
    }

    @Override
    public Uni<List<Customer>> findByCountryResidence(Integer idCountryResidence) {
        return repository.findByCountryResidence(idCountryResidence)
                .onItem().transform(entities -> entities.stream()
                        .map(mapper::toDomain)
                        .toList());
    }

    @Override
    public Uni<List<Customer>> findAllPEP() {
        return repository.findAllPEP()
                .onItem().transform(entities -> entities.stream()
                        .map(mapper::toDomain)
                        .toList());
    }

    @Override
    public Uni<List<Customer>> findByTypeDocument(Integer idTypeDocument) {
        return repository.findByTypeDocument(idTypeDocument)
                .onItem().transform(entities -> entities.stream()
                        .map(mapper::toDomain)
                        .toList());
    }
}
