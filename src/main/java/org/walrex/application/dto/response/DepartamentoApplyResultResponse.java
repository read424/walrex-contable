package org.walrex.application.dto.response;

public record DepartamentoApplyResultResponse(
    String code,
    String ubigeo,
    String status,      // success | error
    String message      // nullable
) {
}
