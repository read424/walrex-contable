package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.walrex.application.dto.request.CreateBeneficiaryAccountRequest;
import org.walrex.application.dto.request.UpdateBeneficiaryAccountRequest;
import org.walrex.domain.model.BeneficiaryAccount;

@Mapper(componentModel = "jakarta", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BeneficiaryAccountRequestMapper {

    @Mapping(source = "customerId", target = "customer.id")
    @Mapping(source = "bankId", target = "bank.id")
    @Mapping(source = "typeAccountId", target = "typeAccount.id")
    @Mapping(source = "typeOperationId", target = "typeOperation.id")
    @Mapping(source = "accountNumber", target = "accountNumber")
    @Mapping(source = "beneficiaryLastName", target = "beneficiaryLastName")
    @Mapping(source = "beneficiarySurname", target = "beneficiarySurname")
    @Mapping(source = "idNumber", target = "idNumber")
    @Mapping(source = "isAccountMe", target = "isAccountMe")
    BeneficiaryAccount toModel(CreateBeneficiaryAccountRequest request);

    @Mapping(source = "customerId", target = "customer.id")
    @Mapping(source = "bankId", target = "bank.id")
    @Mapping(source = "typeAccountId", target = "typeAccount.id")
    @Mapping(source = "typeOperationId", target = "typeOperation.id")
    @Mapping(source = "accountNumber", target = "accountNumber")
    @Mapping(source = "beneficiaryLastName", target = "beneficiaryLastName")
    @Mapping(source = "beneficiarySurname", target = "beneficiarySurname")
    @Mapping(source = "idNumber", target = "idNumber")
    @Mapping(source = "isAccountMe", target = "isAccountMe")
    @Mapping(source = "status", target = "status")
    BeneficiaryAccount toModel(UpdateBeneficiaryAccountRequest request);
}
