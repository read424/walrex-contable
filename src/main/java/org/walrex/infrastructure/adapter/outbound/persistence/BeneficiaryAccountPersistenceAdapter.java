package org.walrex.infrastructure.adapter.outbound.persistence;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.walrex.application.dto.query.BeneficiaryAccountFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.port.output.BeneficiaryAccountQueryPort;
import org.walrex.application.port.output.BeneficiaryAccountRepositoryPort;
import org.walrex.domain.model.BeneficiaryAccount;
import org.walrex.domain.model.PagedResult;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.BeneficiaryAccountEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.BeneficiaryAccountMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.BeneficiaryAccountPanacheRepository;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class BeneficiaryAccountPersistenceAdapter implements BeneficiaryAccountRepositoryPort, BeneficiaryAccountQueryPort {

    @Inject
    BeneficiaryAccountPanacheRepository panacheRepository;

    @Inject
    BeneficiaryAccountMapper persistenceMapper;

    @Override
    public Uni<BeneficiaryAccount> save(BeneficiaryAccount beneficiaryAccount) {
        BeneficiaryAccountEntity entity = persistenceMapper.toEntity(beneficiaryAccount);
        return panacheRepository.persistAndFlush(entity)
                .map(persistenceMapper::toDomain);
    }

    @Override
    public Uni<BeneficiaryAccount> update(BeneficiaryAccount beneficiaryAccount) {
        return panacheRepository.findById(beneficiaryAccount.getId())
                .onItem().ifNotNull().transformToUni(existingEntity -> {
                    BeneficiaryAccountEntity updatedInfoEntity = persistenceMapper.toEntity(beneficiaryAccount);
                    existingEntity.setPayoutRailId(updatedInfoEntity.getPayoutRailId());
                    existingEntity.setBankId(updatedInfoEntity.getBankId());
                    existingEntity.setAccountNumber(updatedInfoEntity.getAccountNumber());
                    existingEntity.setPhoneNumber(updatedInfoEntity.getPhoneNumber());
                    existingEntity.setCurrencyId(updatedInfoEntity.getCurrencyId());
                    existingEntity.setIsFavorite(updatedInfoEntity.getIsFavorite());
                    return panacheRepository.persist(existingEntity).map(persistenceMapper::toDomain);
                });
    }

    @Override
    public Uni<Boolean> softDelete(Long id) {
        return panacheRepository.deleteById(id);
    }

    @Override
    public Uni<PagedResult<BeneficiaryAccount>> findAll(PageRequest pageRequest, BeneficiaryAccountFilter filter) {
        StringBuilder queryBuilder = new StringBuilder("1 = 1");
        Map<String, Object> params = new HashMap<>();

        if (filter != null) {
            if (filter.getCustomerId() != null) {
                // Now it's linked to beneficiary, which is linked to client (customerId in old model, clientId in new)
                queryBuilder.append(" AND beneficiary.clientId = :clientId");
                params.put("clientId", filter.getCustomerId().intValue());
            }
            if (StringUtils.isNotBlank(filter.getAccountNumber())) {
                queryBuilder.append(" AND accountNumber LIKE :accountNumber");
                params.put("accountNumber", "%" + filter.getAccountNumber() + "%");
            }
        }

        Sort.Direction direction = pageRequest.getSortDirection() == PageRequest.SortDirection.ASCENDING ?
                Sort.Direction.Ascending : Sort.Direction.Descending;
        Sort sort = Sort.by(pageRequest.getSortBy(), direction);

        Uni<List<BeneficiaryAccountEntity>> listUni = panacheRepository.find(queryBuilder.toString(), sort, params)
                .page(pageRequest.getPage(), pageRequest.getSize()).list();
                
        Uni<Long> countUni = panacheRepository.count(queryBuilder.toString(), params);

        return Uni.combine().all().unis(listUni, countUni).asTuple()
                .map(tuple -> {
                    List<BeneficiaryAccount> domainList = persistenceMapper.toDomainList(tuple.getItem1());
                    long totalElements = tuple.getItem2();
                    return PagedResult.of(domainList, pageRequest.getPage(), pageRequest.getSize(), totalElements);
                });
    }

    @Override
    public Uni<Optional<BeneficiaryAccount>> findById(Long id) {
        return panacheRepository.findById(id)
                .map(entity -> Optional.ofNullable(entity).map(persistenceMapper::toDomain));
    }

    @Override
    public Uni<Boolean> existsByAccountNumber(String accountNumber, Long excludeId) {
        if (excludeId == null) {
            return panacheRepository.count("accountNumber", accountNumber)
                    .map(count -> count > 0);
        } else {
            return panacheRepository.count("accountNumber = :accountNumber and id != :excludeId",
                    Parameters.with("accountNumber", accountNumber).and("excludeId", excludeId))
                    .map(count -> count > 0);
        }
    }
}