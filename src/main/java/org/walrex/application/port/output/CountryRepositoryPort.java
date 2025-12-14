package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Country;

public interface CountryRepositoryPort {

    /**
     * Persiste un nuevo pais.
     *
     * SQL esperado:
     * INSERT INTO country (...) VALUES ($1, $2, ...) RETURNING *
     *
     * @param country Entidad de dominio a persistir
     * @return Uni con el pais persistida (incluye timestamps del servidor)
     */
    Uni<Country> save(Country country);

    /**
     * Actualiza un pais existente.
     *
     * SQL esperado:
     * UPDATE country SET alphabetic_code=$1, ... , updated_at=NOW()
     * WHERE id=$n AND deleted_at IS NULL RETURNING *
     *
     * @param country Entidad de dominio con los datos actualizados
     * @return Uni con el pais actualizado
     */
    Uni<Country> update(Country country);

    /**
     * Elimina lógicamente un pais (soft delete).
     *
     * SQL esperado:
     * UPDATE country SET deleted_at=NOW(), status='0', updated_at=NOW()
     * WHERE id=$1 AND deleted_at IS NULL
     *
     * @param id Identificador del pais
     * @return Uni<Boolean> true si se eliminó (rowCount > 0), false si no existía
     */
    Uni<Boolean> softDelete(Integer id);

    /**
     * Elimina físicamente un pais (hard delete).
     *
     * SQL esperado:
     * DELETE FROM country WHERE id=$1
     *
     * ⚠️ Usar con precaución. Preferir softDelete.
     *
     * @param id Identificador de la moneda
     * @return Uni<Boolean> true si se eliminó, false si no existía
     */
    Uni<Boolean> hardDelete(Integer id);

    /**
     * Restaura un pais previamente eliminada.
     *
     * SQL esperado:
     * UPDATE country SET deleted_at=NULL, status='1', updated_at=NOW()
     * WHERE id=$1 AND deleted_at IS NOT NULL
     *
     * @param id Identificador del pais
     * @return Uni<Boolean> true si se restauró, false si no existía o no estaba eliminada
     */
    Uni<Boolean> restore(Integer id);
}
