package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BeneficiaryFilter {
    private Integer clientId;
    private String search;
    private Boolean favorites;
}
