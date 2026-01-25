package org.walrex.application.port.output;

import org.walrex.domain.model.RegistrationToken;

public interface RegistrationTokenPort {

    RegistrationToken generate(String target, String purpose);

    boolean validate(String token, String expectedTarget);
}
