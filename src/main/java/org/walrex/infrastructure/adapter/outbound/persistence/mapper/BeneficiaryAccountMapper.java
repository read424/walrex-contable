package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.walrex.domain.model.BeneficiaryAccount;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.BeneficiaryAccountEntity;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface BeneficiaryAccountMapper {
    BeneficiaryAccount toDomain(BeneficiaryAccountEntity entity);
    BeneficiaryAccountEntity toEntity(BeneficiaryAccount domain);
    List<BeneficiaryAccount> toDomainList(List<BeneficiaryAccountEntity> entities);
    List<BeneficiaryAccountEntity> toEntityList(List<BeneficiaryAccount> domains);

    // Métodos de conversión para timestamps
    default OffsetDateTime map(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atOffset(ZoneOffset.UTC);
    }

    default LocalDateTime map(OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : offsetDateTime.toLocalDateTime();
    }
}
