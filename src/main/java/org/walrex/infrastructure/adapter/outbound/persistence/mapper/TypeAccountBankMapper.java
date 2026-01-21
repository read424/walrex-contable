package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.walrex.domain.model.TypeAccountBank;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.TypeAccountBankEntity;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface TypeAccountBankMapper {
    TypeAccountBank toDomain(TypeAccountBankEntity entity);
    TypeAccountBankEntity toEntity(TypeAccountBank domain);
    List<TypeAccountBank> toDomainList(List<TypeAccountBankEntity> entities);
    List<TypeAccountBankEntity> toEntityList(List<TypeAccountBank> domains);
}
