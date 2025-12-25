package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.AccountingAccount;

/**
 * Puerto de salida para operaciones de escritura en cuentas contables.
 *
 * Siguiendo el patrón hexagonal, este puerto define las operaciones
 * de persistencia sin especificar la tecnología (PostgreSQL, etc.).
 */
public interface AccountingAccountRepositoryPort {

    /**
     * Persiste una nueva cuenta contable.
     *
     * SQL esperado:
     * INSERT INTO accountingAccounts (...) VALUES ($1, $2, ...) RETURNING *
     *
     * @param accountingAccountingAccount Entidad de dominio a persistir
     * @return Uni con la cuenta persistida (incluye timestamps del servidor)
     */
    Uni<AccountingAccount> save(AccountingAccount accountingAccountingAccount);

    /**
     * Actualiza una cuenta existente.
     *
     * SQL esperado:
     * UPDATE accountingAccounts SET code=$1, name=$2, ... , updated_at=NOW()
     * WHERE id=$n AND deleted_at IS NULL RETURNING *
     *
     * @param accountingAccountingAccount Entidad de dominio con los datos actualizados
     * @return Uni con la cuenta actualizada
     */
    Uni<AccountingAccount> update(AccountingAccount accountingAccountingAccount);

    /**
     * Elimina lógicamente una cuenta (soft delete).
     *
     * SQL esperado:
     * UPDATE accountingAccounts SET deleted_at=NOW(), is_active=false, updated_at=NOW()
     * WHERE id=$1 AND deleted_at IS NULL
     *
     * @param id Identificador de la cuenta
     * @return Uni<Boolean> true si se eliminó (rowCount > 0), false si no existía
     */
    Uni<Boolean> softDelete(Integer id);

    /**
     * Elimina físicamente una cuenta (hard delete).
     *
     * SQL esperado:
     * DELETE FROM accountingAccounts WHERE id=$1
     *
     * ⚠️ Usar con precaución. Preferir softDelete.
     *
     * @param id Identificador de la cuenta
     * @return Uni<Boolean> true si se eliminó, false si no existía
     */
    Uni<Boolean> hardDelete(Integer id);

    /**
     * Restaura una cuenta previamente eliminada.
     *
     * SQL esperado:
     * UPDATE accountingAccounts SET deleted_at=NULL, is_active=true, updated_at=NOW()
     * WHERE id=$1 AND deleted_at IS NOT NULL
     *
     * @param id Identificador de la cuenta
     * @return Uni<Boolean> true si se restauró, false si no existía o no estaba eliminada
     */
    Uni<Boolean> restore(Integer id);
}
