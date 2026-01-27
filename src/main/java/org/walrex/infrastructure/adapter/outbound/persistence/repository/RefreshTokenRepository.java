package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.RefreshTokenEntity;

@ApplicationScoped
public class RefreshTokenRepository implements PanacheRepository<RefreshTokenEntity> {
}
