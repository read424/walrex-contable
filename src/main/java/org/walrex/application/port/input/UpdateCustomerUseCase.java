package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Customer;

public interface UpdateCustomerUseCase {

    /**
     * Actualiza un cliente existente con nuevos datos.
     *
     * @param id       Identificador del cliente a actualizar
     * @param customer Nuevos datos para el cliente
     * @return Uni con el cliente actualizado
     * @throws org.walrex.domain.exception.CustomerNotFoundException
     * si no existe un cliente con el ID proporcionado
     * @throws org.walrex.domain.exception.DuplicateCustomerException
     * si los nuevos datos entran en conflicto con otro cliente existente
     */
    Uni<Customer> actualizar(Integer id, Customer customer);
}
