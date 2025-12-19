package org.walrex.application.dto.response;

import lombok.Builder;

@Builder
public record DepartamentResponse(
        Integer id,
        String codigo,
        String nombre,
        Boolean status
) {
}
