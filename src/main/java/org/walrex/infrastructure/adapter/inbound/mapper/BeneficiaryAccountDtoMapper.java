package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.walrex.application.dto.response.BeneficiaryAccountResponse;
import org.walrex.domain.model.BeneficiaryAccount;

import java.util.List;

@Mapper(componentModel = "jakarta", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BeneficiaryAccountDtoMapper {

    @Mapping(source = "beneficiary.id", target = "beneficiaryId")
    BeneficiaryAccountResponse toResponse(BeneficiaryAccount beneficiaryAccount);

    List<BeneficiaryAccountResponse> toResponseList(List<BeneficiaryAccount> beneficiaryAccounts);
}
