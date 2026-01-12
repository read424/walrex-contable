package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Modelo de dominio para atributos de producto.
 *
 * Representa un atributo que puede ser asignado a productos (ej: color, talla, material).
 * El atributo define el tipo de entrada (select, radio, color picker, text).
 *
 * Soporta soft delete mediante el campo deletedAt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttribute {

    /**
     * Identificador único del atributo (auto-generado)
     */
    private Integer id;

    /**
     * Nombre descriptivo del atributo
     * Máximo 100 caracteres
     */
    private String name;

    /**
     * Tipo de visualización del atributo
     * SELECT, RADIO, COLOR, TEXT
     */
    private AttributeDisplayType displayType;

    /**
     * Indica si el atributo está activo
     * Los atributos inactivos no se pueden usar en nuevos productos
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
     * Verifica si el atributo está eliminado (soft delete)
     *
     * @return true si el atributo ha sido eliminado lógicamente
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Verifica si el atributo está activo y no eliminado
     *
     * @return true si el atributo está activo y no ha sido eliminado
     */
    public boolean isUsable() {
        return Boolean.TRUE.equals(active) && !isDeleted();
    }
}
