package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.RemittanceRouteEntity;

import java.util.List;

/**
 * Repositorio reactivo para rutas de remesas
 */
@ApplicationScoped
public class RemittanceRouteRepository implements PanacheRepositoryBase<RemittanceRouteEntity, Integer> {

    /**
     * Obtiene todas las rutas activas con country_currencies y sus relaciones cargadas
     */
    public Uni<List<RemittanceRouteEntity>> findAllActiveWithDetails() {
        return find(
                "SELECT r FROM RemittanceRouteEntity r " +
                        "JOIN FETCH r.remittanceCountry rc " +
                        "JOIN FETCH rc.country " +
                        "JOIN FETCH r.countryCurrencyFrom ccFrom " +
                        "JOIN FETCH ccFrom.country " +
                        "JOIN FETCH ccFrom.currency " +
                        "JOIN FETCH r.countryCurrencyTo ccTo " +
                        "JOIN FETCH ccTo.country " +
                        "JOIN FETCH ccTo.currency " +
                        "WHERE r.isActive = '1' AND rc.isActive = '1'"
        ).list();
    }

    /**
     * Obtiene todas las rutas activas con country_currencies y sus relaciones para exchange rates.
     * No incluye JOIN FETCH rc.country ya que no se necesita para tasas de cambio.
     */
    public Uni<List<RemittanceRouteEntity>> findAllActiveExchangeRateRoutes() {
        return find(
                "SELECT r FROM RemittanceRouteEntity r " +
                        "JOIN FETCH r.remittanceCountry rc " +
                        "JOIN FETCH r.countryCurrencyFrom ccFrom " +
                        "JOIN FETCH ccFrom.country " +
                        "JOIN FETCH ccFrom.currency " +
                        "JOIN FETCH r.countryCurrencyTo ccTo " +
                        "JOIN FETCH ccTo.country " +
                        "JOIN FETCH ccTo.currency " +
                        "WHERE r.isActive = '1' AND rc.isActive = '1'"
        ).list();
    }

    /**
     * Busca una ruta específica entre dos country_currencies
     */
    public Uni<RemittanceRouteEntity> findByCountryCurrencies(
            Long idCountryCurrencyFrom,
            Long idCountryCurrencyTo,
            String intermediaryAsset) {
        return find(
                "countryCurrencyFrom.id = ?1 AND countryCurrencyTo.id = ?2 AND intermediaryAsset = ?3 AND isActive = '1'",
                idCountryCurrencyFrom, idCountryCurrencyTo, intermediaryAsset
        ).firstResult();
    }
}
