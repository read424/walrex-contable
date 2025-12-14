package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Data;

/**
 * Objeto para configuración de paginación y ordenamiento.
 *
 * Siguiendo el patrón hexagonal, este DTO pertenece a la capa de aplicación
 * y es independiente de la implementación de persistencia.
 */
@Data
@Builder
public class PageRequest {

    /**
     * Número de página (0-indexed).
     * Por defecto 0.
     */
    @Builder.Default
    private int page = 0;

    /**
     * Tamaño de página (número de elementos por página).
     * Por defecto 20.
     */
    @Builder.Default
    private int size = 20;

    /**
     * Campo por el cual ordenar.
     * Por defecto "id".
     */
    @Builder.Default
    private String sortBy = "id";

    /**
     * Dirección del ordenamiento.
     * Por defecto ASCENDING.
     */
    @Builder.Default
    private SortDirection sortDirection = SortDirection.ASCENDING;

    /**
     * Enum para la dirección del ordenamiento.
     */
    public enum SortDirection {
        ASCENDING("asc"),
        DESCENDING("desc");

        private final String value;

        SortDirection(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static SortDirection fromString(String value) {
            if (value == null) {
                return ASCENDING;
            }
            return switch (value.toLowerCase()) {
                case "desc", "descending" -> DESCENDING;
                default -> ASCENDING;
            };
        }
    }
}
