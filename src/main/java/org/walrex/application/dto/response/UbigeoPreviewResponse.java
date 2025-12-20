package org.walrex.application.dto.response;

import java.util.List;

public record UbigeoPreviewResponse(
    List<DepartamentoPreview> departamento,
    List<ProvinciaPreview> provincia,
    List<DistritoPreview> distrito
) {
}
