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
        entity.createdAt = java.time.LocalDateTime.now();
        entity.updatedAt = java.time.LocalDateTime.now();
        return panacheRepository.persistAndFlush(entity)
                .map(persistenceMapper::toDomain);
    }

    @Override
    public Uni<BeneficiaryAccount> update(BeneficiaryAccount beneficiaryAccount) {
        return panacheRepository.findById(beneficiaryAccount.getId())
                .onItem().ifNotNull().transformToUni(existingEntity -> {
                    // Map updates from domain to existing entity
                    BeneficiaryAccountEntity updatedInfoEntity = persistenceMapper.toEntity(beneficiaryAccount);
                    existingEntity.customer = updatedInfoEntity.customer;
                    existingEntity.bank = updatedInfoEntity.bank;
                    existingEntity.typeAccount = updatedInfoEntity.typeAccount;
                    existingEntity.accountNumber = updatedInfoEntity.accountNumber;
                    existingEntity.beneficiaryLastName = updatedInfoEntity.beneficiaryLastName;
                    existingEntity.beneficiarySurname = updatedInfoEntity.beneficiarySurname;
                    existingEntity.idNumber = updatedInfoEntity.idNumber;
                    existingEntity.status = updatedInfoEntity.status;
                    existingEntity.typeOperation = updatedInfoEntity.typeOperation;
                    existingEntity.isAccountMe = updatedInfoEntity.isAccountMe;
                    existingEntity.updatedAt = java.time.LocalDateTime.now();
                    return panacheRepository.persist(existingEntity).map(persistenceMapper::toDomain);
                });
    }

    @Override
    public Uni<Boolean> softDelete(Integer id) {
        return panacheRepository.findById(id)
                .onItem().ifNotNull().transformToUni(account -> {
                    account.setStatus("0");
                    account.updatedAt = java.time.LocalDateTime.now();
                    return panacheRepository.persist(account)
                            .call(() -> panacheRepository.flush())
                            .onItem().transform(p -> true);
                })
                .onItem().ifNull().continueWith(false);
    }

    @Override
    public Uni<PagedResult<BeneficiaryAccount>> findAll(PageRequest pageRequest, BeneficiaryAccountFilter filter) {
        StringBuilder queryBuilder = new StringBuilder("1 = 1");
        Map<String, Object> params = new HashMap<>();

        if (filter != null) {
            if (filter.getCustomerId() != null) {
                queryBuilder.append(" AND customer.id = :customerId");
                params.put("customerId", filter.getCustomerId());
            }
            if (StringUtils.isNotBlank(filter.getAccountNumber())) {
                queryBuilder.append(" AND accountNumber LIKE :accountNumber");
                params.put("accountNumber", "%" + filter.getAccountNumber() + "%");
            }
            if (StringUtils.isNotBlank(filter.getIdNumber())) {
                queryBuilder.append(" AND idNumber = :idNumber");
                params.put("idNumber", filter.getIdNumber());
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
                    int totalPages = pageRequest.getSize() > 0 ? (int) Math.ceil((double) totalElements / pageRequest.getSize()) : 0;
                    return new PagedResult<>(
                            domainList,
                            pageRequest.getPage(),
                            pageRequest.getSize(),
                            totalElements,
                            totalPages
                    );
                });
    }

    @Override
    public Uni<Optional<BeneficiaryAccount>> findById(Integer id) {
        return panacheRepository.findById(id)
                .map(entity -> Optional.ofNullable(entity).map(persistenceMapper::toDomain));
    }

    @Override
    public Uni<Boolean> existsByAccountNumber(String accountNumber, Integer excludeId) {
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