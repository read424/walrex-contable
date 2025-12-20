package org.walrex.application.dto.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ProvinciaPreview {
    private Long id;
    private String code;
    private String nombre;
    private String ubigeo;
    private String status;
}
