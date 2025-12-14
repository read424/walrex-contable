package org.walrex.domain.model;

import lombok.*;

import java.time.OffsetDateTime;

/**
 * Domain model representing a System Document Type.
 * This entity defines the types of documents that can be used in the system,
 * such as ID cards, passports, tax IDs, etc.
 */
@Builder
@Data
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class SystemDocumentType {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean isRequired;
    private Boolean forPerson;
    private Boolean forCompany;
    private Integer priority;
    private Boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
