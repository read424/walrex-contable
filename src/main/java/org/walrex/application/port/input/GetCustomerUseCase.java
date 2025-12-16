package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Customer;

public interface GetCustomerUseCase {
    /**
     * Obtiene un cliente por su ID.
     *
     * @param id Identificador único del cliente
     * @return Uni con el cliente encontrado
     * @throws com.walrex.domain.exception.CustomerNotFoundException
     * si no existe un cliente con el ID proporcionado
     */
    Uni<Customer> findById(Integer id);
}
