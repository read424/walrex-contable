package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DeviceTokenEntity;

import java.util.List;

@ApplicationScoped
public class DeviceTokenRepository implements PanacheRepositoryBase<DeviceTokenEntity, Integer> {

    public Uni<List<DeviceTokenEntity>> findActiveByUserId(Integer userId) {
        return list("userId = ?1 and active = true", userId);
    }

    public Uni<DeviceTokenEntity> findByToken(String token) {
        return find("token", token).firstResult();
    }

    public Uni<List<DeviceTokenEntity>> findAllActive() {
        return list("active", true);
    }
}
