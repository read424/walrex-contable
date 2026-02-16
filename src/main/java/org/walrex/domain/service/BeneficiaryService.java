package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.query.BeneficiaryFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.BeneficiarySearchResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.input.*;
import org.walrex.application.port.output.BeneficiaryQueryPort;
import org.walrex.application.port.output.BeneficiaryRepositoryPort;
import org.walrex.domain.model.Beneficiary;

import java.util.List;

@ApplicationScoped
public class BeneficiaryService implements 
        CreateBeneficiaryUseCase, 
        UpdateBeneficiaryUseCase, 
        DeleteBeneficiaryUseCase, 
        GetBeneficiaryUseCase, 
        ListBeneficiaryUseCase {

    @Inject
    BeneficiaryRepositoryPort repositoryPort;

    @Inject
    BeneficiaryQueryPort queryPort;

    @Override
    public Uni<Beneficiary> create(Beneficiary beneficiary) {
        return repositoryPort.save(beneficiary);
    }

    @Override
    public Uni<Beneficiary> update(Long id, Beneficiary beneficiary) {
        beneficiary.setId(id);
        return repositoryPort.update(beneficiary);
    }

    @Override
    public Uni<Void> delete(Long id) {
        return repositoryPort.deleteById(id);
    }

    @Override
    public Uni<Beneficiary> findById(Long id) {
        return repositoryPort.findById(id);
    }

    @Override
    public Uni<PagedResponse<BeneficiarySearchResponse>> list(PageRequest pageRequest, BeneficiaryFilter filter) {
        return queryPort.findAll(pageRequest, filter)
                .map(pagedResult -> {
                    List<BeneficiarySearchResponse> maskedContent = pagedResult.content().stream()
                            .map(this::maskAccount)
                            .toList();
                    return PagedResponse.of(
                            maskedContent,
                            pagedResult.page() + 1,
                            pagedResult.size(),
                            pagedResult.totalElements()
                    );
                });
    }

    private BeneficiarySearchResponse maskAccount(BeneficiarySearchResponse res) {
        String accountNumber = res.maskedAccount();
        String masked = accountNumber;
        if (accountNumber != null && accountNumber.length() > 4) {
            masked = "***" + accountNumber.substring(accountNumber.length() - 4);
        } else if (accountNumber == null) {
            masked = "";
        }
        
        return BeneficiarySearchResponse.builder()
                .beneficiaryId(res.beneficiaryId())
                .accountId(res.accountId())
                .fullName(res.fullName())
                .alias(res.alias())
                .rail(res.rail())
                .bank(res.bank())
                .maskedAccount(masked)
                .isFavorite(res.isFavorite())
                .build();
    }
}
