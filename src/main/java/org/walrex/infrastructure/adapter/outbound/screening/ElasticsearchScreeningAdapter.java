package org.walrex.infrastructure.adapter.outbound.screening;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.walrex.application.port.output.ScreeningPort;
import org.walrex.domain.model.ScreeningResult;
import org.walrex.infrastructure.adapter.outbound.screening.dto.ElasticsearchResponse;
import org.walrex.infrastructure.adapter.outbound.screening.dto.ElasticsearchSearchRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ApplicationScoped
public class ElasticsearchScreeningAdapter implements ScreeningPort {

    @RestClient
    ElasticsearchRestClient restClient;

    @ConfigProperty(name = "screening.threshold.block", defaultValue = "8.0")
    double blockThreshold;

    @ConfigProperty(name = "screening.threshold.review", defaultValue = "4.0")
    double reviewThreshold;

    @Override
    public Uni<ScreeningResult> screen(String fullName, String documentNumber) {
        ElasticsearchSearchRequest request = buildQuery(fullName, documentNumber);
        log.debug("Screening request for fullName='{}', document='{}'", fullName, documentNumber);

        return restClient.search(request)
                .onItem().transform(response -> processResponse(response, documentNumber))
                .onFailure().recoverWithItem(e -> {
                    log.error("Elasticsearch screening failed: {}", e.getMessage(), e);
                    return ScreeningResult.builder()
                            .decision("PENDING")
                            .score(BigDecimal.ZERO)
                            .identifierMatched(false)
                            .build();
                });
    }

    private ElasticsearchSearchRequest buildQuery(String fullName, String documentNumber) {
        List<Map<String, Object>> shouldClauses = new ArrayList<>();

        if (fullName != null && !fullName.isBlank()) {
            shouldClauses.add(Map.of(
                    "match", Map.of("full_name", Map.of("query", fullName, "fuzziness", "AUTO"))
            ));
        }
        if (documentNumber != null && !documentNumber.isBlank()) {
            shouldClauses.add(Map.of("term", Map.of("identifiers", documentNumber)));
        }

        Map<String, Object> bool = new HashMap<>();
        bool.put("must", List.of(Map.of("term", Map.of("status", 1))));
        bool.put("should", shouldClauses);
        bool.put("minimum_should_match", 1);

        return new ElasticsearchSearchRequest(20, Map.of("bool", bool));
    }

    private ScreeningResult processResponse(ElasticsearchResponse response, String documentNumber) {
        if (response == null || response.getHits() == null
                || response.getHits().getHits() == null
                || response.getHits().getHits().isEmpty()) {
            return ScreeningResult.builder()
                    .decision("CLEAR")
                    .score(BigDecimal.ZERO)
                    .identifierMatched(false)
                    .build();
        }

        ElasticsearchResponse.Hit topHit = response.getHits().getHits().get(0);
        double rawScore = topHit.getScore() != null ? topHit.getScore() : 0.0;

        ElasticsearchResponse.HitSource source = topHit.getSource();
        List<String> identifiers = (source != null && source.getIdentifiers() != null)
                ? source.getIdentifiers() : List.of();
        List<String> datasets = (source != null && source.getDatasets() != null)
                ? source.getDatasets() : List.of();
        String entityId = topHit.getId();

        boolean identifierMatched = documentNumber != null
                && identifiers.stream().anyMatch(id -> id.equalsIgnoreCase(documentNumber));

        String decision;
        if (identifierMatched || rawScore >= blockThreshold) {
            decision = "BLOCK";
        } else if (rawScore >= reviewThreshold) {
            decision = "REVIEW";
        } else {
            decision = "CLEAR";
        }

        String datasetsStr = datasets.isEmpty() ? null : String.join(",", datasets);

        log.debug("Screening result: decision={}, score={}, identifierMatched={}", decision, rawScore, identifierMatched);

        return ScreeningResult.builder()
                .decision(decision)
                .score(BigDecimal.valueOf(rawScore))
                .datasets(datasetsStr)
                .entityId(entityId)
                .identifierMatched(identifierMatched)
                .build();
    }
}
