package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.User;

public interface UserRepositoryPort {
    Uni<Long> save(User user);
}
