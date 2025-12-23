package org.walrex.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CategoryProductSelectResponse {
    private Integer id;
    private String name;
    private Integer parentId;
    private String description;
    private Boolean hasChildren;
    private Integer childrenCount;
}
