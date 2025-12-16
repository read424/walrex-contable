package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.request.CreateCustomerRequest;
import org.walrex.application.dto.request.UpdateCustomerRequest;
import org.walrex.domain.model.Customer;

@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface CustomerRequestMapper {
    /**
     * Convierte un CreateCustomerRequest a Customer.
     *
     * Mapeo de campos:
     * - id -> id
     * - idTypeDocument -> idTypeDocument
     * - numberDocument -> numberDocument
     * - firstName -> firstName
     * - lastName -> lastName
     * - email -> email
     * - phoneNumber -> phoneNumber
     * - idCountryDepartment, idCountryProvince, idCountryDistrict -> no se mapean
     * directamente
     * - address -> no existe en Customer
     *
     * Los campos de Customer que no están en CreateCustomerRequest se ignorarán:
     * - gender, birthDate, idProfessional, isPEP, idCountryResidence,
     * idCountryPhone
     * - createdAt, updatedAt, deletedAt (campos de auditoría)
     *
     * @param request DTO de entrada desde el REST handler
     * @return Customer para el caso de uso
     */
    Customer toModel(CreateCustomerRequest request);

    /**
     * Convierte un UpdateCustomerRequest a Customer.
     *
     * @param request DTO de entrada para actualización
     * @return Customer para el caso de uso
     */
    Customer toModel(UpdateCustomerRequest request);
}
