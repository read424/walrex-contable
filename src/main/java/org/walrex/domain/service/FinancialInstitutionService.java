package org.walrex.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.response.FinancialInstitutionResponse;
import org.walrex.application.dto.response.RequiredFieldAdditionalResponse;
import org.walrex.application.port.input.GetFinancialInstitutionsUseCase;
import org.walrex.application.port.output.FinancialInstitutionQueryPort;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.InstitutionPayoutRailEntity;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class FinancialInstitutionService implements GetFinancialInstitutionsUseCase {

    @Inject
    FinancialInstitutionQueryPort queryPort;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Uni<List<FinancialInstitutionResponse>> getByMethodAndCountry(String methodType, String countryIso2) {
        log.debug("Fetching financial institutions for method: {} and country: {}", methodType, countryIso2);
        
        return queryPort.findByRailCodeAndCountryIso2(methodType, countryIso2)
                .map(entities -> entities.stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()));
    }

    private FinancialInstitutionResponse mapToResponse(InstitutionPayoutRailEntity entity) {
        RequiredFieldAdditionalResponse requiredFields = parseRequiredFields(entity.getRequiredFields());
        
        return FinancialInstitutionResponse.builder()
                .id_financial_institution(entity.getBank().getId())
                .name_financial_institution(entity.getBank().getDetName())
                .siglas_financial_institution(entity.getBank().getSigla())
                .required_fields_additional(List.of(requiredFields))
                .build();
    }

    private RequiredFieldAdditionalResponse parseRequiredFields(String json) {
        if (json == null || json.isBlank()) {
            return new RequiredFieldAdditionalResponse();
        }
        try {
            return objectMapper.readValue(json, RequiredFieldAdditionalResponse.class);
        } catch (Exception e) {
            log.error("Error parsing required_fields JSON: {}", json, e);
            return new RequiredFieldAdditionalResponse();
        }
    }
}
