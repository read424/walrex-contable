package org.walrex.domain.model;

public record CategoryProductWithChildren(
        Integer id,
        String name,
        String details,
        Integer parentId,
        Boolean hasChildren,
        Integer childrenCount
) {}
