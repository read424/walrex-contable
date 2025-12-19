package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.request.LoadDataINEIRequest;
import org.walrex.application.dto.response.LoadUbigeoDataResponse;

public interface LoadUbigeoDataUseCase {
    Uni<LoadUbigeoDataResponse> loadData(LoadDataINEIRequest request);
}
