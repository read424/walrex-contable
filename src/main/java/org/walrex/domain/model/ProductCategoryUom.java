package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Modelo de dominio para categorías de unidades de medida.
 *
 * Representa una categoría que agrupa unidades de medida relacionadas
 * (ej: longitud, peso, volumen, etc.).
 *
 * Soporta soft delete mediante el campo deletedAt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryUom {

    /**
     * Identificador único de la categoría
     */
    private Integer id;

    /**
     * Código único de la categoría (ej: LENGTH, WEIGHT, VOLUME)
     * Máximo 20 caracteres, formato libre
     */
    private String code;

    /**
     * Nombre descriptivo de la categoría
     * Máximo 100 caracteres
     */
    private String name;

    /**
     * Descripción detallada de la categoría
     * Máximo 255 caracteres
     */
    private String description;

    /**
     * Indica si la categoría está activa
     * Las categorías inactivas no se pueden usar en nuevas unidades de medida
     */
    private Boolean active;

    /**
     * Fecha de creación del registro
     */
    private OffsetDateTime createdAt;

    /**
     * Fecha de última actualización del registro
     */
    private OffsetDateTime updatedAt;

    /**
     * Fecha de eliminación lógica del registro
     * Si es null, el registro no ha sido eliminado
     */
    private OffsetDateTime deletedAt;

    /**
     * Verifica si la categoría está eliminada (soft delete)
     *
     * @return true si la categoría ha sido eliminada lógicamente
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Verifica si la categoría está activa y no eliminada
     *
     * @return true si la categoría está activa y no ha sido eliminada
     */
    public boolean isUsable() {
        return Boolean.TRUE.equals(active) && !isDeleted();
    }
}
