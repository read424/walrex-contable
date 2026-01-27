package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepositoryPort {
    Uni<Optional<RefreshToken>> findByTokenHash(String tokenHash);
    Uni<RefreshToken> save(RefreshToken refreshToken);
    Uni<RefreshToken> update(RefreshToken refreshToken);
}
