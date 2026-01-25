package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.Mapper;
import org.walrex.application.dto.request.OcupacionCreateRequest;
import org.walrex.application.dto.request.OcupacionUpdateRequest;
import org.walrex.application.dto.response.OcupacionResponse;
import org.walrex.domain.model.Ocupacion;

@Mapper(componentModel = "cdi")
public interface OcupacionRestMapper {

    Ocupacion toDomain(OcupacionCreateRequest request);
    Ocupacion toDomain(OcupacionUpdateRequest request);

    OcupacionResponse toResponse(Ocupacion domain);

}
