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
public class CountryRateTypesResponse {
    private CountryInfoResponse country;
    private List<ExchangeRateTypeResponse> rateTypes;
}