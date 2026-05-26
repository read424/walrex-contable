package org.walrex.domain.service;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.CustomerRepositoryPort;
import org.walrex.application.port.output.ScreeningHistoryRepositoryPort;
import org.walrex.application.port.output.ScreeningPort;
import org.walrex.domain.model.ClientScreeningHistory;
import org.walrex.domain.model.ScreeningResult;

import java.time.OffsetDateTime;

@Slf4j
@ApplicationScoped
public class ScreeningService {

    @Inject
    ScreeningPort screeningPort;

    @Inject
    ScreeningHistoryRepositoryPort historyPort;

    @Inject
    CustomerRepositoryPort customerRepositoryPort;

    /**
     * Ejecuta el screening de un cliente contra Elasticsearch y persiste el resultado.
     * La llamada HTTP a ES ocurre fuera de la transacción; los writes a DB se envuelven
     * en su propia transacción reactiva mediante Panache.withTransaction().
     */
    public Uni<ScreeningResult> screenCustomer(Integer clientId, String fullName, String documentNumber) {
        log.info("Starting screening for clientId={}", clientId);

        return screeningPort.screen(fullName, documentNumber)
                .onItem().transformToUni(result ->
                        Panache.withTransaction(() -> {
                            OffsetDateTime checkedAt = OffsetDateTime.now();

                            ClientScreeningHistory history = ClientScreeningHistory.builder()
                                    .clientId(clientId)
                                    .decision(result.getDecision())
                                    .score(result.getScore())
                                    .datasets(result.getDatasets())
                                    .entityId(result.getEntityId())
                                    .identifierMatched(result.isIdentifierMatched())
                                    .triggeredBy("AUTO")
                                    .checkedAt(checkedAt)
                                    .build();

                            return historyPort.save(history)
                                    .chain(() -> customerRepositoryPort.updateScreeningResult(
                                            clientId,
                                            result.getDecision(),
                                            result.getScore(),
                                            result.getDatasets(),
                                            result.getEntityId(),
                                            checkedAt
                                    ))
                                    .replaceWith(result);
                        })
                )
                .invoke(result -> log.info("Screening completed for clientId={}: decision={}, score={}",
                        clientId, result.getDecision(), result.getScore()))
                .onFailure().invoke(e -> log.error("Screening failed for clientId={}: {}", clientId, e.getMessage(), e));
    }
}
