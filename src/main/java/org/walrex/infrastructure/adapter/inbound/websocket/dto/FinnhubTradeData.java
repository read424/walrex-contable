package org.walrex.infrastructure.adapter.inbound.websocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinnhubTradeData {

    /** Precio del trade */
    @JsonProperty("p")
    private BigDecimal price;

    /** Símbolo (ej: OANDA:PEN_BRL) */
    @JsonProperty("s")
    private String symbol;

    /** Timestamp en milisegundos (epoch) */
    @JsonProperty("t")
    private Long timestamp;

    /** Volumen */
    @JsonProperty("v")
    private BigDecimal volume;
}
