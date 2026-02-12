package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.request.GenerateQrRequest;
import org.walrex.application.dto.response.DecodeQrResponse;

public interface EmvQrUseCase {
    Uni<String> generateQr(GenerateQrRequest request);
    Uni<DecodeQrResponse> decodeQr(String qrCodeText);
}
