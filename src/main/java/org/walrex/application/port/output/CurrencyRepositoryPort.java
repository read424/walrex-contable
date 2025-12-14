package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Currency;

public interface CurrencyRepositoryPort {

    /**
     * Persiste una nueva moneda.
     *
     * SQL esperado:
     * INSERT INTO currencies (...) VALUES ($1, $2, ...) RETURNING *
     *
     * @param currency Entidad de dominio a persistir
     * @return Uni con la moneda persistida (incluye timestamps del servidor)
     */
    Uni<Currency> save(Currency currency);

    /**
     * Actualiza una moneda existente.
     *
     * SQL esperado:
     * UPDATE currencies SET alphabetic_code=$1, ... , updated_at=NOW()
     * WHERE id=$n AND deleted_at IS NULL RETURNING *
     *
     * @param currency Entidad de dominio con los datos actualizados
     * @return Uni con la moneda actualizada
     */
    Uni<Currency> update(Currency currency);

    /**
     * Elimina lógicamente una moneda (soft delete).
     *
     * SQL esperado:
     * UPDATE currencies SET deleted_at=NOW(), active=false, updated_at=NOW()
     * WHERE id=$1 AND deleted_at IS NULL
     *
     * @param id Identificador de la moneda
     * @return Uni<Boolean> true si se eliminó (rowCount > 0), false si no existía
     */
    Uni<Boolean> softDelete(Integer id);

    /**
     * Elimina físicamente una moneda (hard delete).
     *
     * SQL esperado:
     * DELETE FROM currencies WHERE id=$1
     *
     * ⚠️ Usar con precaución. Preferir softDelete.
     *
     * @param id Identificador de la moneda
     * @return Uni<Boolean> true si se eliminó, false si no existía
     */
    Uni<Boolean> hardDelete(Integer id);

    /**
     * Restaura una moneda previamente eliminada.
     *
     * SQL esperado:
     * UPDATE currencies SET deleted_at=NULL, active=true, updated_at=NOW()
     * WHERE id=$1 AND deleted_at IS NOT NULL
     *
     * @param id Identificador de la moneda
     * @return Uni<Boolean> true si se restauró, false si no existía o no estaba eliminada
     */
    Uni<Boolean> restore(Integer id);
}
