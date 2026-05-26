package org.walrex.infrastructure.adapter.outbound.astropay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload para POST /v1/public/exchanges
 * AstroPay requiere los códigos en minúsculas: {"from":"pen","to":"eur"}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AstroPayExchangeRequest {

    private String from;

    private String to;
}
