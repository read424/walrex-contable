package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.RemittanceCountryEntity;

import java.util.List;

/**
 * Repositorio reactivo para países habilitados en remesas
 */
@ApplicationScoped
public class RemittanceCountryRepository implements PanacheRepositoryBase<RemittanceCountryEntity, Integer> {

    /**
     * Obtiene todos los países activos para remesas con sus datos cargados
     */
    public Uni<List<RemittanceCountryEntity>> findAllActive() {
        return find(
                "SELECT rc FROM RemittanceCountryEntity rc " +
                        "JOIN FETCH rc.country " +
                        "WHERE rc.isActive = '1'"
        ).list();
    }

    /**
     * Busca un país por su ID en la tabla country
     */
    public Uni<RemittanceCountryEntity> findByCountryId(Integer idCountry) {
        return find(
                "SELECT rc FROM RemittanceCountryEntity rc " +
                        "JOIN FETCH rc.country c " +
                        "WHERE c.id = ?1 AND rc.isActive = '1'",
                idCountry
        ).firstResult();
    }
}
