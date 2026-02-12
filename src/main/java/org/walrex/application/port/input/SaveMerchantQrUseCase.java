package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.request.SaveMerchantQrRequest;
import org.walrex.domain.model.MerchantQr;

import java.util.List;

public interface SaveMerchantQrUseCase {
    Uni<MerchantQr> saveMerchantQr(SaveMerchantQrRequest request);
    Uni<List<MerchantQr>> getAllMerchantQrs();
    Uni<String> generateFromProfile(Long id, java.math.BigDecimal amount);
    Uni<Boolean> deleteMerchantQr(Long id);
}
