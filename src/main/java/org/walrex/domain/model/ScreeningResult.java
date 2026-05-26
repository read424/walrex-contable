package org.walrex.domain.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Builder
@Data
@AllArgsConstructor
@ToString
public class ScreeningResult {

    private String decision;

    private BigDecimal score;

    private String datasets;

    private String entityId;

    private boolean identifierMatched;
}
