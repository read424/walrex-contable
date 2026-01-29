package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Customer;

import java.util.Optional;

public interface ClientRepositoryPort {
    Uni<Integer> save(Customer customer);

    Uni<Optional<Customer>> findById(Integer id);
}
