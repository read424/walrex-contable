package org.walrex.application.dto.request;

import java.util.List;

public record LoadDataINEIRequest(
        List<UgibeoINEIRowRequest> records
) {
}
