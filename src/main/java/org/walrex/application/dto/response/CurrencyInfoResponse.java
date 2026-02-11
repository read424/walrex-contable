package org.walrex.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyInfoResponse {
    private String code;
    private String name;
    private String symbol;
    //private List<ExchangeRateInfoResponse> rates;
}