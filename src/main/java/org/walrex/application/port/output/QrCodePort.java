package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.EmvQrCode;

public interface QrCodePort {
    Uni<String> encode(EmvQrCode data);
    Uni<EmvQrCode> decode(String qrCode);
}
