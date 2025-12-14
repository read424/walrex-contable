package org.walrex.domain.model;

import lombok.*;

import java.time.OffsetDateTime;

@Builder
@Data
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Currency {

    private Integer id;
    private String alphabeticCode;
    private Integer numericCode;
    private String name;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
