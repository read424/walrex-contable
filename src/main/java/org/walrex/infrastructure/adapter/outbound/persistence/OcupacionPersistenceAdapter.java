package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.port.output.OcupacionRepositoryPort;
import org.walrex.domain.model.Ocupacion;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.OcupacionPersistenceMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.OcupacionEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.OcupacionRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class OcupacionPersistenceAdapter implements OcupacionRepositoryPort {

    @Inject
    OcupacionRepository ocupacionRepository;

    @Inject
    OcupacionPersistenceMapper ocupacionPersistenceMapper;

    @Override
    public Uni<Ocupacion> save(Ocupacion ocupacion) {
        OcupacionEntity entity = ocupacionPersistenceMapper.toEntity(ocupacion);
        return ocupacionRepository.persist(entity)
                .map(ocupacionPersistenceMapper::toDomain);
    }

    @Override
    public Uni<Ocupacion> update(Ocupacion ocupacion) {
        return ocupacionRepository.findById(ocupacion.getId())
                .onItem().ifNotNull().transformToUni(entity -> {
                    ocupacionPersistenceMapper.updateEntityFromDomain(ocupacion, entity);
                    return ocupacionRepository.persistAndFlush(entity)
                            .map(ocupacionPersistenceMapper::toDomain);
                });
    }

    @Override
    public Uni<Void> delete(Long id) {
        return ocupacionRepository.deleteById(id).replaceWithVoid();
    }

    @Override
    public Uni<Optional<Ocupacion>> findById(Long id) {
        return ocupacionRepository.findById(id)
                .map(entity -> Optional.ofNullable(entity).map(ocupacionPersistenceMapper::toDomain));
    }

    @Override
    public Uni<List<Ocupacion>> findAll(Integer page, Integer size, String nombreFilter) {
        return ocupacionRepository.findAllPaginated(page, size, nombreFilter)
                .map(entities -> entities.stream()
                        .map(ocupacionPersistenceMapper::toDomain)
                        .collect(Collectors.toList()));
    }

    @Override
    public Uni<List<Ocupacion>> findAllNoPaginated(String nombreFilter) {
        return ocupacionRepository.findAllNoPaginated(nombreFilter)
                .map(entities -> entities.stream()
                        .map(ocupacionPersistenceMapper::toDomain)
                        .collect(Collectors.toList()));
    }

    @Override
    public Uni<Optional<Ocupacion>> findByCodigo(String codigo) {
        return ocupacionRepository.findByCodigo(codigo)
                .map(entity -> Optional.ofNullable(entity).map(ocupacionPersistenceMapper::toDomain));
    }

    @Override
    public Uni<Optional<Ocupacion>> findByNombre(String nombre) {
        return ocupacionRepository.findByNombre(nombre)
                .map(entity -> Optional.ofNullable(entity).map(ocupacionPersistenceMapper::toDomain));
    }
}
