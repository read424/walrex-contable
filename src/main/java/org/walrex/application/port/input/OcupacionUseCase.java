package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.OcupacionResponse;
import java.util.List;
import org.walrex.application.dto.request.OcupacionCreateRequest;
import org.walrex.domain.model.Ocupacion;

public interface OcupacionUseCase {

    Uni<Ocupacion> createOcupacion(OcupacionCreateRequest request);
    Uni<Ocupacion> updateOcupacion(Long id, Ocupacion ocupacion);
    Uni<Void> deleteOcupacion(Long id);
    Uni<Ocupacion> findOcupacionById(Long id);
    Uni<List<OcupacionResponse>> findAllOcupaciones(Integer page, Integer size, String nombreFilter);
    Uni<List<OcupacionResponse>> findAllOcupacionesNoPaginated(String nombreFilter);
}
