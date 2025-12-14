package org.walrex.application.dto.response;

import java.util.List;

/**
 * DTO genérico para respuestas paginadas.
 *
 * Sigue el estándar de Spring Data pero adaptado a Quarkus.
 * Incluye metadatos de paginación útiles para el frontend.
 *
 * @param <T> Tipo de elemento en la lista
 */
public record PagedResponse<T>(

        /**
         * Lista de elementos en la página actual.
         */
        List<T> content,

        /**
         * Número de página actual (1-indexed para el frontend).
         */
        int page,

        /**
         * Tamaño de página solicitado.
         */
        int size,

        /**
         * Total de elementos en todas las páginas.
         */
        long totalElements,

        /**
         * Total de páginas disponibles.
         */
        int totalPages,

        /**
         * Indica si es la primera página.
         */
        boolean first,

        /**
         * Indica si es la última página.
         */
        boolean last,

        /**
         * Indica si hay contenido en esta página.
         */
        boolean empty

) {
    /**
     * Factory method para crear una respuesta paginada.
     * Expects page to be 1-indexed (first page = 1).
     */
    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        return new PagedResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page == 1,                          // first (1-indexed)
                page >= totalPages,                 // last (1-indexed)
                content.isEmpty()                    // empty
        );
    }
}
