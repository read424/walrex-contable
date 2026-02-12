package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.MerchantQr;

import java.util.List;

public interface MerchantQrOutputPort {
    Uni<MerchantQr> save(MerchantQr merchantQr);
    Uni<MerchantQr> findById(Long id);
    Uni<List<MerchantQr>> findAll();
    Uni<Boolean> delete(Long id);
}
