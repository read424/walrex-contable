package org.walrex.application.dto.response;

public record ProvinciaApplyResultResponse(
    String code,
    String codParent,
    String ubigeo,
    String status,      // success | error
    String message
) {
}
