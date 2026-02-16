package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.walrex.application.dto.request.CreateBeneficiaryAccountRequest;
import org.walrex.application.dto.request.UpdateBeneficiaryAccountRequest;
import org.walrex.domain.model.BeneficiaryAccount;

@Mapper(componentModel = "jakarta", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BeneficiaryAccountRequestMapper {

    @Mapping(source = "beneficiaryId", target = "beneficiary.id")
    BeneficiaryAccount toModel(CreateBeneficiaryAccountRequest request);

    BeneficiaryAccount toModel(UpdateBeneficiaryAccountRequest request);
}
