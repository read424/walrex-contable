package org.walrex.infrastructure.adapter.outbound.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemittanceDestinationDto {
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
}