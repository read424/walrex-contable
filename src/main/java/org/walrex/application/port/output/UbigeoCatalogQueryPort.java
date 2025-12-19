package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;

import java.util.Map;

public interface UbigeoCatalogPort {
    Uni<Map<String, Long>> departamentosExistentes(); // code -> id
    Uni<Map<String, Long>> provinciasExistentes();     // code -> id
    Uni<Map<String, Long>> distritosExistentes();      // ubigeo -> id
}
