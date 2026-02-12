package org.walrex.infrastructure.adapter.outbound.persistence.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class RemittanceRouteResultDto {
    private String codeIsoFrom;
    private String nameIsoFrom;
    private String symbolIsoFrom;
    private String codeIsoTo;
    private String nameIsoTo;
    private String symbolIsoTo;
    private String countryIso2;
    private String countryIso3;
    private String countryName;
    private String countryFlag;
    private Integer rateTypesCount;

    // Constructor para usar con createNativeQuery
    public RemittanceRouteResultDto(String codeIsoFrom, String nameIsoFrom, String symbolIsoFrom,
                                   String codeIsoTo, String nameIsoTo, String symbolIsoTo,
                                   String countryIso2, String countryIso3, String countryName,
                                   String countryFlag, Integer rateTypesCount) {
        this.codeIsoFrom = codeIsoFrom;
        this.nameIsoFrom = nameIsoFrom;
        this.symbolIsoFrom = symbolIsoFrom;
        this.codeIsoTo = codeIsoTo;
        this.nameIsoTo = nameIsoTo;
        this.symbolIsoTo = symbolIsoTo;
        this.countryIso2 = countryIso2;
        this.countryIso3 = countryIso3;
        this.countryName = countryName;
        this.countryFlag = countryFlag;
        this.rateTypesCount = rateTypesCount;
    }
}