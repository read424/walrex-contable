package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.domain.model.OtpPurpose;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.OtpEntity;

@ApplicationScoped
public class OtpPanacheRepository implements PanacheRepository<OtpEntity> {

    public Uni<OtpEntity> findValidByReferenceId(String referenceId, OtpPurpose purpose) {
        return find(
                "referenceId = ?1 and purpose = ?2 and used = false",
                referenceId,
                purpose
        ).firstResult();
    }
}
