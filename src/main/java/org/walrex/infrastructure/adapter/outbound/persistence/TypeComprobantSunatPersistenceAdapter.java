package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.port.output.TypeComprobantSunatQueryPort;
import org.walrex.domain.model.TypeComprobantSunat;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.TypeComprobantSunatMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.TypeComprobantSunatRepository;

import java.util.List;

/**
 * Persistence adapter that implements output ports for TypeComprobantSunat.
 *
 * Following the hexagonal pattern (Ports & Adapters), this adapter:
 * - Implements port interfaces (TypeComprobantSunatQueryPort)
 * - Translates between domain model (TypeComprobantSunat) and persistence layer (TypeComprobantSunatEntity)
 * - Uses mapper for transformations
 * - Delegates persistence operations to Panache repositories
 */
@ApplicationScoped
public class TypeComprobantSunatPersistenceAdapter implements TypeComprobantSunatQueryPort {

    @Inject
    TypeComprobantSunatRepository repository;

    @Inject
    TypeComprobantSunatMapper mapper;

    @Override
    public Uni<List<TypeComprobantSunat>> findAll() {
        return repository.findAllSorted()
                .onItem().transform(mapper::toDomainList);
    }
}
