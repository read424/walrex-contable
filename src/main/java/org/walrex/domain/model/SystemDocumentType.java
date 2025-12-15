package org.walrex.domain.model;

import lombok.*;

import java.time.OffsetDateTime;

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
