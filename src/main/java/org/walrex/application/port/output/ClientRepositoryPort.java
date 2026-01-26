package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Customer;

public interface ClientRepositoryPort {
    Uni<Integer> save(Customer customer);
}
