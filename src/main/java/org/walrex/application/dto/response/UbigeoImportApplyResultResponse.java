package org.walrex.application.dto.response;

import java.util.List;

public record UbigeoImportApplyResultResponse(
    List<DepartamentoApplyResultResponse> departamento,
    List<ProvinciaApplyResultResponse> provincia,
    List<DistritoApplyResultResponse> distrito
) {
}
