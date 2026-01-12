package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Modelo de dominio para unidades de medida de productos.
 *
 * Representa una unidad de medida específica dentro de una categoría
 * (ej: metro, kilómetro dentro de "longitud"; gramo, kilogramo dentro de "peso").
 *
 * Incluye factor de conversión y precisión de redondeo para cálculos.
 * Soporta soft delete mediante el campo deletedAt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUom {

    /**
     * Identificador único de la unidad de medida
     */
    private Integer id;

    /**
     * Código único de la unidad de medida (ej: M, KM, G, KG)
     * Máximo 10 caracteres, formato libre
     */
    private String codeUom;

    /**
     * Nombre descriptivo de la unidad de medida
     * Máximo 100 caracteres
     */
    private String nameUom;

    /**
     * ID de la categoría a la que pertenece esta unidad de medida
     */
    private Integer categoryId;

    /**
     * Factor de conversión a la unidad base de la categoría
     * Por defecto 1.0
     * Ejemplo: 1 km = 1000 metros (factor = 1000.0)
     */
    private BigDecimal factor;

    /**
     * Precisión de redondeo para cálculos con esta unidad
     * Por defecto 0.01
     */
    private BigDecimal roundingPrecision;

    /**
     * Indica si la unidad de medida está activa
     * Las unidades inactivas no se pueden usar en nuevos productos
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
     * Verifica si la unidad de medida está eliminada (soft delete)
     *
     * @return true si la unidad ha sido eliminada lógicamente
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Verifica si la unidad de medida está activa y no eliminada
     *
     * @return true si la unidad está activa y no ha sido eliminada
     */
    public boolean isUsable() {
        return Boolean.TRUE.equals(active) && !isDeleted();
    }
}
