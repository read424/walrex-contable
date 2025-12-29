package org.walrex.application.dto.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para response de Binance P2P API
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceP2PResponse {
    private String code;
    private String message;
    private String messageDetail;
    private List<P2PData> data;
    private Integer total;
    private Boolean success;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class P2PData {
        private Adv adv;
        private Advertiser advertiser;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Adv {
            private String advNo;
            private String classify;
            private String tradeType;
            private String asset;
            private String fiatUnit;
            private String price;
            private String surplusAmount;
            private String minSingleTransAmount;
            private String maxSingleTransAmount;
            private Integer payTimeLimit;
            private List<TradeMethod> tradeMethods;
            private Boolean isTradable;
            private String privilegeDesc;  // Si es != null, filtrar
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Advertiser {
            private String nickName;
            private Integer monthOrderCount;
            private Integer monthFinishRate;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TradeMethod {
            private String tradeMethodId;
            private String tradeMethodName;
            private String tradeMethodShortName;
        }
    }
}
