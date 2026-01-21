package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.walrex.application.dto.response.BeneficiaryAccountResponse;
import org.walrex.domain.model.BeneficiaryAccount;

import java.util.List;

@Mapper(componentModel = "jakarta", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BeneficiaryAccountDtoMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.firstName", target = "customerName") // Assuming Customer has a firstName field
    @Mapping(source = "bank.id", target = "bankId")
    @Mapping(source = "bank.detName", target = "bankName")
    @Mapping(source = "typeAccount.id", target = "typeAccountId")
    @Mapping(source = "typeAccount.name", target = "typeAccountName")
    @Mapping(source = "typeOperation.id", target = "typeOperationId")
    @Mapping(source = "typeOperation.name", target = "typeOperationName")
    BeneficiaryAccountResponse toResponse(BeneficiaryAccount beneficiaryAccount);

    List<BeneficiaryAccountResponse> toResponseList(List<BeneficiaryAccount> beneficiaryAccounts);
}
