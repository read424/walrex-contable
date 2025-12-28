package org.walrex.infrastructure.adapter.inbound.rest;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.walrex.domain.service.ExchangeRateService;

/**
 * Endpoint temporal para testear manualmente la actualización de tasas de cambio
 * Este endpoint ejecuta en el event loop thread, por lo que Hibernate Reactive funcionará correctamente
 */
@Slf4j
@Path("/api/admin/exchange-rates")
public class ExchangeRateTestResource {

    @Inject
    ExchangeRateService exchangeRateService;

    /**
     * Trigger manual para actualizar tasas de cambio
     * GET http://localhost:8089/api/admin/exchange-rates/update
     */
    @GET
    @Path("/update")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> updateExchangeRates() {
        log.info("=== Manual trigger: Starting exchange rate update ===");

        return exchangeRateService.updateExchangeRates()
                .onItem().transform(update -> {
                    String message = String.format(
                            "✅ Exchange rates updated successfully! Processed %d currency pairs",
                            update.ratesByPair().size()
                    );
                    log.info(message);
                    return message;
                })
                .onFailure().recoverWithItem(failure -> {
                    String error = String.format(
                            "❌ Failed to update exchange rates: %s",
                            failure.getMessage()
                    );
                    log.error(error, failure);
                    return error;
                });
    }
}
