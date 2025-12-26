package org.walrex.domain.model;

import lombok.*;

/**
 * Domain model representing a Type of Comprobant (document) from SUNAT.
 * Represents fiscal document types like invoices, receipts, etc.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class TypeComprobantSunat {
    private Integer id;
    private String sunatCode;
    private String nameDocument;
}
