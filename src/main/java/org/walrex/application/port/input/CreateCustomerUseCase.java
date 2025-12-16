package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Customer;

public interface CreateCustomerUseCase {
    /**
     * Crea un nuevo cliente en el sistema.
     *
     * @param customer Datos necesarios para crear el cliente
     * @return Uni con el cliente creado
     * @throws org.walrex.domain.exception.DuplicateCustomerException
     * si ya existe un cliente con el mismo número de documento
     * @throws org.walrex.domain.exception.InvalidCustomerDataException
     * si los datos no cumplen las reglas de negocio
     */
    Uni<Customer> agregar(Customer customer);
}
