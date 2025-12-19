package org.walrex.application.dto.response;

import lombok.Builder;

@Builder
public record DistrictResponse(
        Integer id,
        String codigo,
        String name,
        ProvinceResponse province,
        Boolean status
) {
}
