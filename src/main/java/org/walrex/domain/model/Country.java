package org.walrex.domain.model;

import lombok.*;

import java.time.OffsetDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class Country {

    private Integer id;
    private String alphabeticCode2;
    private String alphabeticCode3;
    private Integer numericCode;
    private String name;
    private String phoneCode;
    private String status;
    private String unicodeFlag;
    private OffsetDateTime createdAt;
    private  OffsetDateTime updatedAt;
}
