package org.walrex.infrastructure.adapter.outbound.security;

import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.domain.service.OtpGenerator;

import java.security.SecureRandom;

@ApplicationScoped
public class SecureRandomOtpGenerator implements OtpGenerator {

    @Override
    public String generate() {
        int otp = 100_000 + new SecureRandom().nextInt(900_000);
        return String.valueOf(otp);
    }
}
