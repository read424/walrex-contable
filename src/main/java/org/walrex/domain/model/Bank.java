package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Domain model for banks/payment methods
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Bank {
    private Long id;
    private String sigla;
    private String detName;
    private Integer idCountry;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String codigo;
    private String namePayBinance;
}
