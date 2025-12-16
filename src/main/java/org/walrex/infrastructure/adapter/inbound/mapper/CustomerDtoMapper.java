package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.response.CustomerResponse;
import org.walrex.domain.model.Customer;

import java.util.List;

/**
 * Mapper entre el modelo de dominio Customer y los DTOs de la capa de
 * aplicación.
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de
 * compilación.
 *
 * Configuración:
 * - componentModel = "cdi": Integración con CDI de Quarkus (permite @Inject)
 * - unmappedTargetPolicy = IGNORE: Ignora campos no mapeados
 * - injectionStrategy = CONSTRUCTOR: Usa inyección por constructor (mejor para
 * inmutabilidad)
 *
 * Responsabilidades:
 * - Convertir Customer (dominio) → CustomerResponse (DTO salida)
 */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI, unmappedTargetPolicy = ReportingPolicy.IGNORE, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CustomerDtoMapper {

    /**
     * Convierte el modelo de dominio a DTO de respuesta.
     *
     * Mapeo directo de todos los campos excepto deletedAt que se ignora
     * (no se expone al exterior por seguridad).
     *
     * @param customer Modelo de dominio
     * @return CustomerResponse DTO de respuesta
     */
    CustomerResponse toResponse(Customer customer);

    /**
     * Convierte una lista de Customer a lista de CustomerResponse.
     *
     * @param customers Lista de modelos de dominio
     * @return Lista de DTOs de respuesta
     */
    List<CustomerResponse> toResponseList(List<Customer> customers);
}
