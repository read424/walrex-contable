package org.walrex.application.dto.response;

import lombok.Builder;

@Builder
public record ProvinceResponse(
        Integer id,
        String codigo,
        String name,
        DepartamentResponse departament,
        Boolean status
) {
}
