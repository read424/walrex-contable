package org.walrex.domain.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.BeneficiaryAccountFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.BeneficiaryAccountResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.input.*;
import org.walrex.application.port.output.BeneficiaryAccountCachePort;
import org.walrex.application.port.output.BeneficiaryAccountQueryPort;
import org.walrex.application.port.output.BeneficiaryAccountRepositoryPort;
import org.walrex.domain.exception.BeneficiaryAccountNotFoundException;
import org.walrex.domain.exception.DuplicateBeneficiaryAccountException;
import org.walrex.domain.model.BeneficiaryAccount;
import org.walrex.infrastructure.adapter.inbound.mapper.BeneficiaryAccountDtoMapper;
import org.walrex.infrastructure.adapter.outbound.cache.BeneficiaryAccountCacheKeyGenerator;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.BeneficiaryAccountCache;

import java.time.Duration;
import java.util.List;

@Slf4j
@Transactional
@ApplicationScoped
public class BeneficiaryAccountService implements
        CreateBeneficiaryAccountUseCase,
        UpdateBeneficiaryAccountUseCase,
        DeleteBeneficiaryAccountUseCase,
        GetBeneficiaryAccountUseCase,
        ListBeneficiaryAccountUseCase {

    @Inject
    BeneficiaryAccountRepositoryPort repositoryPort;

    @Inject
    BeneficiaryAccountQueryPort queryPort;

    @Inject
    BeneficiaryAccountDtoMapper dtoMapper;

    @Inject
    @BeneficiaryAccountCache
    BeneficiaryAccountCachePort cachePort;

    @Override
    public Uni<BeneficiaryAccount> create(BeneficiaryAccount beneficiaryAccount) {
        log.info("Creating beneficiary account for customer ID: {}", beneficiaryAccount.getCustomer().getId());
        return validateUniqueness(beneficiaryAccount.getAccountNumber(), null)
                .onItem().transformToUni(v -> repositoryPort.save(beneficiaryAccount))
                .onItem().call(cachePort::invalidateAll);
    }

    @Override
    public Uni<BeneficiaryAccount> update(Integer id, BeneficiaryAccount beneficiaryAccount) {
        log.info("Updating beneficiary account id: {}", id);
        return validateUniqueness(beneficiaryAccount.getAccountNumber(), id)
                .onItem().transformToUni(v -> {
                    beneficiaryAccount.setId(id);
                    return repositoryPort.update(beneficiaryAccount);
                })
                .onItem().call(cachePort::invalidateAll);
    }

    @Override
    public Uni<Void> delete(Integer id) {
        log.info("Soft deleting beneficiary account id: {}", id);
        return repositoryPort.softDelete(id)
                .onItem().transformToUni(deleted -> {
                    if (!deleted) {
                        return Uni.createFrom().failure(new BeneficiaryAccountNotFoundException(id));
                    }
                    return cachePort.invalidateAll();
                });
    }

    @Override
    public Uni<BeneficiaryAccount> findById(Integer id) {
        // Caching for single items is not implemented in the generic adapter, so we go directly to the DB
        return queryPort.findById(id)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new BeneficiaryAccountNotFoundException(id)));
    }

    @Override
    public Uni<PagedResponse<BeneficiaryAccountResponse>> list(PageRequest pageRequest, BeneficiaryAccountFilter filter) {
        log.info("Listing beneficiary accounts with page: {}, size: {}, filter: {}",
                pageRequest.getPage(), pageRequest.getSize(), filter);

        String cacheKey = BeneficiaryAccountCacheKeyGenerator.generateKey(pageRequest, filter);
        
        Uni<PagedResponse<BeneficiaryAccountResponse>> cachedResponse = cachePort.get(cacheKey);

        return cachedResponse.onItem().ifNull().switchTo(() -> 
            queryPort.findAll(pageRequest, filter)
                .onItem().transformToUni(pagedResult -> {
                    List<BeneficiaryAccountResponse> responses = pagedResult.content().stream()
                            .map(dtoMapper::toResponse)
                            .toList();

                    PagedResponse<BeneficiaryAccountResponse> response = PagedResponse.of(
                            responses,
                            pagedResult.page() + 1,
                            pagedResult.size(),
                            pagedResult.totalElements());
                    
                    return cachePort.put(cacheKey, response, Duration.ofMinutes(10))
                        .replaceWith(response);
                })
        );
    }

    @Override
    public Multi<BeneficiaryAccountResponse> streamAll() {
        // Caching for streams is not part of the current pattern, bypassing.
        return queryPort.findAll(PageRequest.builder().page(0).size(Integer.MAX_VALUE).build(), null)
                .onItem().transformToMulti(pagedResult -> Multi.createFrom().iterable(pagedResult.content()))
                .onItem().transform(dtoMapper::toResponse);
    }

    @Override
    public Multi<BeneficiaryAccountResponse> streamWithFilter(BeneficiaryAccountFilter filter) {
        // Caching for streams is not part of the current pattern, bypassing.
        return queryPort.findAll(PageRequest.builder().page(0).size(Integer.MAX_VALUE).build(), filter)
                .onItem().transformToMulti(pagedResult -> Multi.createFrom().iterable(pagedResult.content()))
                .onItem().transform(dtoMapper::toResponse);
    }

    @Override
    public Uni<List<BeneficiaryAccountResponse>> listByCustomerId(Long customerId) {
        log.info("Listing all beneficiary accounts for customer ID: {}", customerId);

        BeneficiaryAccountFilter filter = BeneficiaryAccountFilter.builder()
                .customerId(customerId)
                .build();

        PageRequest pageRequest = PageRequest.builder()
                .page(0)
                .size(Integer.MAX_VALUE)
                .sortBy("id")
                .sortDirection(PageRequest.SortDirection.ASCENDING)
                .build();

        return queryPort.findAll(pageRequest, filter)
                .map(pagedResult -> pagedResult.content().stream()
                        .map(dtoMapper::toResponse)
                        .toList());
    }

    private Uni<Void> validateUniqueness(String accountNumber, Integer excludeId) {
        return queryPort.existsByAccountNumber(accountNumber, excludeId)
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateBeneficiaryAccountException("accountNumber", accountNumber));
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}