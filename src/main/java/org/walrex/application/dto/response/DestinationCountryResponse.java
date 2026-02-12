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
public class DestinationCountryResponse {
    private CountryInfoResponse country;
    private List<CurrencyInfoResponse> currencies;
    private Integer rateTypesCount;
}