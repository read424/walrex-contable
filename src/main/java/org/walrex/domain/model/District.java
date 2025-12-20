package org.walrex.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Builder
@Data
public class District {
    private Integer id;
    private Province province;
    private String code;
    private String name;
    private Boolean status;
    private OffsetDateTime created_at;
    private OffsetDateTime updated_at;
}
