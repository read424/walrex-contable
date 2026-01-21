package org.walrex.application.port.output;

import org.walrex.application.dto.response.BeneficiaryAccountResponse;

/**
 * Puerto de salida específico para operaciones de caché de BeneficiaryAccount.
 * Extiende el puerto genérico CachePort.
 */
public interface BeneficiaryAccountCachePort extends CachePort<BeneficiaryAccountResponse> {
}
