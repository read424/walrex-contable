package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeAccountBank {
    public Integer id;
    public String name;
    public String status;
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;
}
