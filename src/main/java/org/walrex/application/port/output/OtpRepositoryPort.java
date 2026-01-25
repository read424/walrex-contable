package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Otp;
import org.walrex.domain.model.OtpPurpose;

public interface OtpRepositoryPort {
    Uni<Otp> save(Otp otp);

    Uni<Otp> findValidOtp(String referenceId, OtpPurpose purpose);

    /**
     * Busca un OTP activo (no usado, no expirado) para el mismo target y purpose.
     * Usado para idempotencia: evitar generar múltiples OTPs duplicados.
     */
    Uni<Otp> findActiveByTargetAndPurpose(String target, OtpPurpose purpose);

    Uni<Void> update(Otp otp);
}
