package org.walrex.domain.model;

import java.time.OffsetDateTime;

public record CategoryProduct(
        Long id,
        String name,
        String details,
        Integer parentId,
        OffsetDateTime createdAt
){}
