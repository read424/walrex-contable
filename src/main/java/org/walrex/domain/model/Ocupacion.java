package org.walrex.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ocupacion {

    private Long id;
    private String codigo;
    private String nombre;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
