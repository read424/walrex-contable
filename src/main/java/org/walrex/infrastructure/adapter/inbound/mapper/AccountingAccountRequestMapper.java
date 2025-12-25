package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.request.CreateAccountingAccountRequest;
import org.walrex.application.dto.request.UpdateAccountingAccountRequest;
import org.walrex.domain.model.AccountingAccount;

/**
 * Mapper para convertir DTOs de request a modelos de dominio.
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de compilación.
 *
 * Configuración:
 * - componentModel = "cdi": Integración con CDI de Quarkus (permite @Inject)
 * - unmappedTargetPolicy = IGNORE: Ignora campos no mapeados
 * - injectionStrategy = CONSTRUCTOR: Usa inyección por constructor
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface AccountingAccountRequestMapper {
    /**
     * Convierte un CreateAccountingAccountRequest a AccountingAccount.
     *
     * El mapeo es directo ya que ambos tienen los mismos campos:
     * - code
     * - name
     * - type
     * - normalSide
     * - active
     *
     * @param request DTO de entrada desde el REST handler
     * @return AccountingAccount para el caso de uso
     */
    AccountingAccount toModel(CreateAccountingAccountRequest request);

    /**
     * Convierte un UpdateAccountingAccountRequest a AccountingAccount.
     *
     * @param request DTO de entrada desde el REST handler
     * @return AccountingAccount para el caso de uso
     */
    AccountingAccount toModel(UpdateAccountingAccountRequest request);
}
