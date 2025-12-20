package org.walrex.application.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UbigeoRecord {
    private String id;
    private String departamento;
    private String provincia;
    private String distrito;
    private String codigo_ubigeo;
    private String validationStatus; // 'valid' | 'warning' | 'error'
}
