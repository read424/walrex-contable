package org.walrex.application.dto.response;

public record DistritoApplyResultResponse(
    String codParent,
    String ubigeo,
    String status,      // success | error
    String message
) {
}
