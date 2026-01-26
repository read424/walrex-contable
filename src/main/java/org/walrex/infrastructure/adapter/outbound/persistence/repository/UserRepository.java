package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.UserEntity;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<UserEntity, Integer> {

    /**
     * Busca usuario por username (email o teléfono).
     */
    public Uni<UserEntity> findByUsername(String username) {
        return find("username", username).firstResult();
    }

    /**
     * Verifica si existe un usuario con el username dado.
     */
    public Uni<Boolean> existsByUsername(String username) {
        return count("username", username).map(count -> count > 0);
    }

    /**
     * Busca usuario por clientId.
     */
    public Uni<UserEntity> findByClientId(Integer clientId) {
        return find("clientId", clientId).firstResult();
    }
}
