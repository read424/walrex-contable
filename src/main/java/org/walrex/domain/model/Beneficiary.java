package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Beneficiary {
    private Long id;
    private Integer clientId;
    private Integer countryId;
    private String firstName;
    private String lastName;
    private Integer documentType;
    private String documentNumber;
    private String alias;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
