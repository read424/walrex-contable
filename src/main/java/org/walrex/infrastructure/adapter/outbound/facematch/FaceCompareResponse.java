package org.walrex.infrastructure.adapter.outbound.facematch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta del servicio de comparación facial Python.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceCompareResponse {

    private Double similarity;

    private Boolean match;
}
