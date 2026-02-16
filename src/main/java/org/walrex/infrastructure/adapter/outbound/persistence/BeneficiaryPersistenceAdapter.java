package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.query.BeneficiaryFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.BeneficiarySearchResponse;
import org.walrex.application.port.output.BeneficiaryQueryPort;
import org.walrex.application.port.output.BeneficiaryRepositoryPort;
import org.walrex.domain.model.Beneficiary;
import org.walrex.domain.model.PagedResult;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.BeneficiaryEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.BeneficiaryMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.BeneficiaryAccountPanacheRepository;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.BeneficiaryPanacheRepository;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class BeneficiaryPersistenceAdapter implements BeneficiaryRepositoryPort, BeneficiaryQueryPort {

    @Inject
    BeneficiaryPanacheRepository beneficiaryRepository;

    @Inject
    BeneficiaryAccountPanacheRepository accountRepository;

    @Inject
    BeneficiaryMapper persistenceMapper;

    @Override
    public Uni<Beneficiary> save(Beneficiary beneficiary) {
        BeneficiaryEntity entity = persistenceMapper.toEntity(beneficiary);
        return beneficiaryRepository.persistAndFlush(entity)
                .map(persistenceMapper::toDomain);
    }

    @Override
    public Uni<Beneficiary> findById(Long id) {
        return beneficiaryRepository.findById(id)
                .map(persistenceMapper::toDomain);
    }

    @Override
    public Multi<Beneficiary> findAllByClientId(Integer clientId) {
        return beneficiaryRepository.find("clientId", clientId).list()
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list))
                .map(persistenceMapper::toDomain);
    }

    @Override
    public Uni<Void> deleteById(Long id) {
        return beneficiaryRepository.deleteById(id).replaceWithVoid();
    }

    @Override
    public Uni<Beneficiary> update(Beneficiary beneficiary) {
        return beneficiaryRepository.findById(beneficiary.getId())
                .onItem().ifNotNull().transformToUni(entity -> {
                    entity.setFirstName(beneficiary.getFirstName());
                    entity.setLastName(beneficiary.getLastName());
                    entity.setDocumentType(beneficiary.getDocumentType());
                    entity.setDocumentNumber(beneficiary.getDocumentNumber());
                    entity.setAlias(beneficiary.getAlias());
                    entity.setClientId(beneficiary.getClientId());
                    entity.setCountryId(beneficiary.getCountryId());
                    entity.setStatus(beneficiary.getStatus());
                    return beneficiaryRepository.persist(entity).map(persistenceMapper::toDomain);
                });
    }

    @Override
    public Uni<PagedResult<BeneficiarySearchResponse>> findAll(PageRequest pageRequest, BeneficiaryFilter filter) {
        Uni<List<BeneficiarySearchResponse>> listUni = accountRepository.findUnified(pageRequest, filter);
        Uni<Long> countUni = accountRepository.countUnified(filter);

        return Uni.combine().all().unis(listUni, countUni).asTuple()
                .map(tuple -> PagedResult.of(
                        tuple.getItem1(),
                        pageRequest.getPage(),
                        pageRequest.getSize(),
                        tuple.getItem2()
                ));
    }

    @Override
    public Uni<Optional<Beneficiary>> findOneById(Long id) {
        return beneficiaryRepository.findById(id)
                .map(entity -> Optional.ofNullable(entity).map(persistenceMapper::toDomain));
    }
}
