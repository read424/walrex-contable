package org.walrex.application.port.output;

import org.walrex.application.dto.response.AccountingAccountResponse;

/**
 * Puerto de salida para operaciones de caché de AccountingAccount.
 *
 * Siguiendo el patrón hexagonal, este puerto define las operaciones
 * de caché sin especificar la implementación (Redis).
 */
public interface AccountingAccountCachePort extends CachePort<AccountingAccountResponse> {
}
