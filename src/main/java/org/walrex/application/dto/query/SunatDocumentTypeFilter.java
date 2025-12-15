package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Data;

/**
 * Filtros para búsqueda de tipos de documentos SUNAT.
 *
 * Usado en conjunto con PageRequest para construir consultas paginadas y filtradas.
 * Sigue el patrón Builder para facilitar la construcción de filtros complejos.
 */
@Data
@Builder
public class SunatDocumentTypeFilter {

    /**
     * Búsqueda general en nombre, código o ID.
     * Se aplica con LIKE/ILIKE en múltiples campos.
     */
    private String search;

    /**
     * Filtro exacto por código SUNAT.
     */
    private String code;

    /**
     * Filtro por estado activo/inactivo.
     * - null: devuelve todos
     * - true: solo activos
     * - false: solo inactivos
     */
    private Boolean active;

    /**
     * Filtro por longitud específica.
     * Ejemplo: length=8 para buscar solo documentos de 8 caracteres (DNI)
     */
    private Integer length;

    /**
     * Incluir registros inactivos en la búsqueda.
     * - "1" o null: solo activos
     * - "0": incluye inactivos
     */
    private String includeInactive;

    /**
     * Verifica si se deben incluir registros inactivos.
     *
     * @return true si se deben incluir inactivos, false en caso contrario
     */
    public boolean shouldIncludeInactive() {
        return "0".equals(includeInactive);
    }
}
