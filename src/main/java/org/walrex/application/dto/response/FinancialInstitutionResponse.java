package org.walrex.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialInstitutionResponse {
    private Long id_financial_institution;
    private String name_financial_institution;
    private String siglas_financial_institution;
    private List<RequiredFieldAdditionalResponse> required_fields_additional;
}
