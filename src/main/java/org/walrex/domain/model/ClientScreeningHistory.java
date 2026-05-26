package org.walrex.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@Data
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class ClientScreeningHistory {

    private Integer id;

    private Integer clientId;

    private String decision;

    private BigDecimal score;

    private String datasets;

    private String entityId;

    @Builder.Default
    private Boolean identifierMatched = false;

    @Builder.Default
    private String triggeredBy = "AUTO";

    private OffsetDateTime checkedAt;
}
