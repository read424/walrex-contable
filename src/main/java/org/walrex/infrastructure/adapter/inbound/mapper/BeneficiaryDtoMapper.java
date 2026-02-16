package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.walrex.application.dto.response.BeneficiaryResponse;
import org.walrex.domain.model.Beneficiary;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface BeneficiaryDtoMapper {
    BeneficiaryResponse toResponse(Beneficiary domain);
    Beneficiary toDomain(BeneficiaryResponse response);
    List<BeneficiaryResponse> toResponseList(List<Beneficiary> domains);
}
