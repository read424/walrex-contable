package org.walrex.infrastructure.adapter.outbound.security;

import jakarta.enterprise.context.ApplicationScoped;
import org.mindrot.jbcrypt.BCrypt;
import org.walrex.domain.service.OtpHasher;

@ApplicationScoped
public class BCryptOtpHasher implements OtpHasher {

    @Override
    public String hash(String raw) {
        return BCrypt.hashpw(raw, BCrypt.gensalt());
    }

    @Override
    public boolean matches(String raw, String hash) {
        return BCrypt.checkpw(raw, hash);
    }
}
