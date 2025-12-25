package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.response.AccountingAccountResponse;
import org.walrex.application.dto.response.AccountingAccountSelectResponse;
import org.walrex.domain.model.AccountingAccount;

import java.util.List;

/**
 * Mapper entre el modelo de dominio AccountingAccount y los DTOs de respuesta.
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de compilación.
 *
 * Configuración:
 * - componentModel = "cdi": Integración con CDI de Quarkus (permite @Inject)
 * - unmappedTargetPolicy = IGNORE: Ignora campos no mapeados
 * - injectionStrategy = CONSTRUCTOR: Usa inyección por constructor (mejor para inmutabilidad)
 *
 * Responsabilidades:
 * - Convertir AccountingAccount (dominio) → AccountingAccountResponse (DTO salida)
 * - Convertir AccountingAccount (dominio) → AccountingAccountSelectResponse (DTO optimizado)
 * - No exponer deletedAt al exterior
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface AccountingAccountDtoMapper {

    /**
     * Convierte el modelo de dominio a DTO de respuesta.
     *
     * Mapeo directo de todos los campos excepto deletedAt que no se expone.
     *
     * @param accountingAccountingAccount Modelo de dominio
     * @return AccountingAccountResponse DTO de respuesta
     */
    AccountingAccountResponse toResponse(AccountingAccount accountingAccountingAccount);

    /**
     * Convierte una lista de AccountingAccount a lista de AccountingAccountResponse.
     *
     * @param accountingAccounts Lista de modelos de dominio
     * @return Lista de DTOs de respuesta
     */
    List<AccountingAccountResponse> toResponseList(List<AccountingAccount> accountingAccounts);

    /**
     * Convierte el modelo de dominio a DTO de respuesta optimizado para selects.
     *
     * Solo incluye campos esenciales: id, code, name
     *
     * @param accountingAccountingAccount Modelo de dominio
     * @return AccountingAccountSelectResponse DTO optimizado para selects
     */
    AccountingAccountSelectResponse toSelectResponse(AccountingAccount accountingAccountingAccount);

    /**
     * Convierte una lista de AccountingAccount a lista de AccountingAccountSelectResponse.
     *
     * @param accountingAccounts Lista de modelos de dominio
     * @return Lista de DTOs optimizados para selects
     */
    List<AccountingAccountSelectResponse> toSelectResponseList(List<AccountingAccount> accountingAccounts);
}
