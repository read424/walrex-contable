package org.walrex.infrastructure.adapter.outbound.astropay;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.walrex.application.port.output.AstroPayPort;
import org.walrex.domain.model.AstroPayRate;
import org.walrex.infrastructure.adapter.outbound.astropay.dto.AstroPayExchangeRequest;
import org.walrex.infrastructure.adapter.outbound.astropay.dto.AstroPayExchangeResponse;

@Slf4j
@ApplicationScoped
@RegisterForReflection
public class AstroPayExchangeRateAdapter implements AstroPayPort {

    @RestClient
    AstroPayRestClient restClient;

    @Override
    public Uni<AstroPayRate> getExchangeRate(String from, String to) {
        // AstroPay exige códigos en minúsculas
        String fromLower = from.toLowerCase();
        String toLower   = to.toLowerCase();

        log.info("[AstroPay] Consultando tasa {}→{}", fromLower, toLower);

        return restClient.getExchangeRate(new AstroPayExchangeRequest(fromLower, toLower))
                .onItem().transform(this::toDomain)
                .invoke(rate -> log.info("[AstroPay] Tasa {}→{}: official={} exchange={}",
                        rate.getFrom(), rate.getTo(), rate.getOfficialExchange(), rate.getExchange()))
                .onFailure().invoke(e ->
                        log.error("[AstroPay] Error consultando {}→{}: {}", fromLower, toLower, e.getMessage(), e))
                .onFailure().recoverWithUni(e ->
                        Uni.createFrom().failure(
                                new RuntimeException("AstroPay no disponible para " + from + "→" + to, e)));
    }

    private AstroPayRate toDomain(AstroPayExchangeResponse r) {
        return AstroPayRate.builder()
                .from(r.getFrom())
                .to(r.getTo())
                .officialExchange(r.getOfficialExchange())
                .exchange(r.getExchange())
                .spread(r.getSpread())
                .spreadAmount(r.getSpreadAmount())
                .exchangeWithoutSpread(r.getExchangeWithoutSpread())
                .build();
    }
}
