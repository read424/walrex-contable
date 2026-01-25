package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Optional;
import org.walrex.domain.model.Ocupacion;

public interface OcupacionRepositoryPort {

    Uni<Ocupacion> save(Ocupacion ocupacion);
    Uni<Ocupacion> update(Ocupacion ocupacion);
    Uni<Void> delete(Long id);
    Uni<Optional<Ocupacion>> findById(Long id);
    Uni<List<Ocupacion>> findAll(Integer page, Integer size, String nombreFilter);
    Uni<List<Ocupacion>> findAllNoPaginated(String nombreFilter);
    Uni<Optional<Ocupacion>> findByCodigo(String codigo);
    Uni<Optional<Ocupacion>> findByNombre(String nombre);
}
