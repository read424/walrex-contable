package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.application.dto.command.CreateCurrencyCommand;
import org.walrex.application.dto.request.CreateCurrencyRequest;

/**
 * Mapper para convertir DTOs de Request a Commands.
 *
 * Responsabilidad:
 * - Transformar CreateCurrencyRequest (capa REST) → CreateCurrencyCommand (capa de aplicación)
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
public interface CurrencyCommandMapper {

    /**
     * Convierte un CreateCurrencyRequest a CreateCurrencyCommand.
     *
     * El mapeo es directo ya que ambos tienen los mismos campos:
     * - alphabeticCode
     * - numericCode
     * - name
     *
     * @param request DTO de entrada desde el REST handler
     * @return Command para el caso de uso
     */
    CreateCurrencyCommand toCommand(CreateCurrencyRequest request);
}