package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeOperation {
    public Integer id;
    public String name;
    public Boolean requiresBank;
    public String labelHelper;
    public String status;
}
