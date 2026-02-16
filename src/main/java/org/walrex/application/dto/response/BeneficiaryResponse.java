package org.walrex.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BeneficiaryResponse {
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
