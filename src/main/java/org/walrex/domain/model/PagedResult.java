package org.walrex.domain.model;

import java.util.List;

public record PagedResult<T>(
        /**
         * Lista de elementos en la página actual.
         */
        List<T> content,

        /**
         * Número de página actual (0-indexed).
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
        int totalPages
) {
    /**
     * Factory method para crear un resultado paginado calculando automáticamente totalPages.
     *
     * @param content Lista de elementos
     * @param page Número de página (0-indexed)
     * @param size Tamaño de página
     * @param totalElements Total de elementos
     * @param <T> Tipo de elemento
     * @return PagedResult con totalPages calculado
     */
    public static <T> PagedResult<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new PagedResult<>(content, page, size, totalElements, totalPages);
    }
}