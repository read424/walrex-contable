package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.port.output.OtpRepositoryPort;
import org.walrex.domain.model.Otp;
import org.walrex.domain.model.OtpPurpose;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.OtpEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.OtpMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.OtpPanacheRepository;

@ApplicationScoped
public class OtpPersistenceAdapter implements OtpRepositoryPort {

    @Inject
    OtpMapper mapper;

    @Inject
    OtpPanacheRepository repository;

    @Override
    public Uni<Otp> save(Otp otp) {
        OtpEntity entity = mapper.toEntity(otp);
        return repository.persist(entity)
                .replaceWith(mapper.toDomain(entity));
    }

    @Override
    public Uni<Otp> findValidOtp(String referenceId, OtpPurpose purpose) {
        return repository
                .findValidByReferenceId(referenceId, purpose)
                .map(mapper::toDomain);
    }

    @Override
    public Uni<Void> update(Otp otp) {
        return save(otp).replaceWithVoid();
    }
}
