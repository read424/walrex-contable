package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BeneficiaryAccountFilter {
    private Long customerId;
    private String accountNumber;
}
