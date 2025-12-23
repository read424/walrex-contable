package org.walrex.domain.model;

import java.time.OffsetDateTime;

public record ProductBrand(
        Integer id,
        String name,
        String details,
        OffsetDateTime createdAt
){}
