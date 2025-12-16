package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Customer;

public interface CustomerRepositoryPort {

    /**
     * Persiste un nuevo cliente.
     *
     * SQL esperado:
     * INSERT INTO clients (...) VALUES ($1, $2, ...) RETURNING *
     *
     * @param customer Entidad de dominio a persistir
     * @return Uni con el cliente persistido (incluye timestamps del servidor)
     */
    Uni<Customer> save(Customer customer);

    /**
     * Actualiza un cliente existente.
     *
     * SQL esperado:
     * UPDATE clients SET id_type_document=$1, ... , updated_at=NOW()
     * WHERE id=$n AND deleted_at IS NULL RETURNING *
     *
     * @param customer Entidad de dominio con los datos actualizados
     * @return Uni con el cliente actualizado
     */
    Uni<Customer> update(Customer customer);

    /**
     * Elimina lógicamente un cliente (soft delete).
     *
     * SQL esperado:
     * UPDATE clients SET deleted_at=NOW(), updated_at=NOW()
     * WHERE id=$1 AND deleted_at IS NULL
     *
     * @param id Identificador del cliente
     * @return Uni<Boolean> true si se eliminó (rowCount > 0), false si no existía
     */
    Uni<Boolean> softDelete(Integer id);

    /**
     * Elimina físicamente un cliente (hard delete).
     *
     * SQL esperado:
     * DELETE FROM clients WHERE id=$1
     *
     * ⚠️ Usar con precaución. Preferir softDelete.
     *
     * @param id Identificador del cliente
     * @return Uni<Boolean> true si se eliminó, false si no existía
     */
    Uni<Boolean> hardDelete(Integer id);

    /**
     * Restaura un cliente previamente eliminado.
     *
     * SQL esperado:
     * UPDATE clients SET deleted_at=NULL, updated_at=NOW()
     * WHERE id=$1 AND deleted_at IS NOT NULL
     *
     * @param id Identificador del cliente
     * @return Uni<Boolean> true si se restauró, false si no existía o no estaba
     *         eliminado
     */
    Uni<Boolean> restore(Integer id);
}
