package org.walrex.infrastructure.adapter.outbound.persistence;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.RemittanceRouteOutputPort;
import org.walrex.domain.model.RemittanceRoute;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CurrencyEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.RemittanceRouteEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.CurrencyRepository;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.RemittanceRouteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para consultar rutas de remesas configuradas
 */
@Slf4j
@ApplicationScoped
public class RemittanceRoutePersistenceAdapter implements RemittanceRouteOutputPort {

    @Inject
    RemittanceRouteRepository remittanceRouteRepository;

    @Inject
    CurrencyRepository currencyRepository;

    @Override
    @WithSession
    public Uni<List<RemittanceRoute>> findAllActiveRoutes() {
        log.info("Fetching all active remittance routes");

        return remittanceRouteRepository.findAllActiveWithDetails()
                .onItem().transformToUni(entities -> {
                    if (entities.isEmpty()) {
                        return Uni.createFrom().item(new ArrayList<RemittanceRoute>());
                    }

                    // Recopilar todos los códigos únicos de activos intermediarios
                    java.util.Set<String> intermediaryAssetCodes = entities.stream()
                            .map(RemittanceRouteEntity::getIntermediaryAsset)
                            .collect(java.util.stream.Collectors.toSet());

                    log.debug("Found {} unique intermediary assets: {}",
                            intermediaryAssetCodes.size(), intermediaryAssetCodes);

                    // Consultar TODAS las currencies de los activos intermediarios en una sola query
                    List<Uni<CurrencyEntity>> currencyUnis = intermediaryAssetCodes.stream()
                            .map(code -> currencyRepository.findByAlphabeticCode(code))
                            .toList();

                    // Combinar todas las consultas de currencies
                    return Uni.combine().all().unis(currencyUnis)
                            .with(list -> {
                                // Crear un mapa código -> ID
                                java.util.Map<String, Integer> codeToIdMap = new java.util.HashMap<>();
                                for (Object obj : list) {
                                    CurrencyEntity currency = (CurrencyEntity) obj;
                                    if (currency != null) {
                                        codeToIdMap.put(currency.getAlphabeticCode(), currency.getId());
                                    }
                                }

                                log.debug("Currency code to ID map: {}", codeToIdMap);

                                // Ahora mapear todas las entidades usando el mapa (sin consultas adicionales)
                                return entities.stream()
                                        .map(entity -> mapToModelWithIdMap(entity, codeToIdMap))
                                        .toList();
                            });
                })
                .invoke(routes ->
                        log.info("Found {} active remittance routes", routes.size())
                );
    }

    /**
     * Mapea una entidad de ruta de remesa a modelo de dominio,
     * usando un mapa precargado de códigos de currency a IDs
     */
    private RemittanceRoute mapToModelWithIdMap(
            RemittanceRouteEntity entity,
            java.util.Map<String, Integer> currencyCodeToIdMap) {

        Integer currencyFromId = entity.getCountryCurrencyFrom().getCurrency().getId();
        String currencyFromCode = entity.getCountryCurrencyFrom().getCurrency().getAlphabeticCode();
        Integer currencyToId = entity.getCountryCurrencyTo().getCurrency().getId();
        String currencyToCode = entity.getCountryCurrencyTo().getCurrency().getAlphabeticCode();
        String intermediaryAsset = entity.getIntermediaryAsset();
        String countryFromName = entity.getCountryCurrencyFrom().getCountry().getName();
        String countryToName = entity.getCountryCurrencyTo().getCountry().getName();

        // Obtener el ID del activo intermediario del mapa
        Integer intermediaryAssetId = currencyCodeToIdMap.get(intermediaryAsset);

        if (intermediaryAssetId == null) {
            log.error("Intermediary asset currency not found in map: {}", intermediaryAsset);
            throw new IllegalStateException("Currency not found for intermediary asset: " + intermediaryAsset);
        }

        log.debug("Mapping route: {}-{} (ID:{}) -> {}-{} (ID:{}), intermediary: {} (ID:{})",
                countryFromName, currencyFromCode, currencyFromId,
                countryToName, currencyToCode, currencyToId,
                intermediaryAsset, intermediaryAssetId);

        return new RemittanceRoute(
                currencyFromId,
                currencyFromCode,
                currencyToId,
                currencyToCode,
                intermediaryAssetId,
                intermediaryAsset
        );
    }
}
