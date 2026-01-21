package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Otp;
import org.walrex.domain.model.OtpPurpose;

public interface OtpRepositoryPort {
    Uni<Otp> save(Otp otp);

    Uni<Otp> findValidOtp(String referenceId, OtpPurpose purpose);

    Uni<Void> update(Otp otp);
}
