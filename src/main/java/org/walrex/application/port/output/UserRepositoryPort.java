package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.User;
import java.util.Optional;

public interface UserRepositoryPort {
    Uni<Long> save(User user);
    Uni<Optional<User>> findByUsername(String username);
    Uni<Optional<User>> findById(Integer id);
    Uni<User> update(User user);
}