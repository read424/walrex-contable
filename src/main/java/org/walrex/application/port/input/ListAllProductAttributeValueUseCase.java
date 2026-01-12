package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.ProductAttributeValueFilter;
import org.walrex.application.dto.response.ProductAttributeValueSelectResponse;

import java.util.List;

/**
 * Puerto de entrada (Input Port) para listar todos los valores de atributos de producto sin paginación.
 *
 * Este caso de uso encapsula la lógica para obtener listados completos:
 * 1. Aplicar filtros opcionales (attributeId, active, etc.)
 * 2. Intentar obtener del caché (TTL mayor: 15 minutos)
 * 3. Si no está en caché, consultar DB y cachear resultado
 * 4. Retornar lista completa en formato optimizado (SelectResponse)
 *
 * Casos de uso típicos:
 * - Llenar dropdowns/selects en formularios
 * - Obtener todos los valores de un atributo específico (ej: todas las tallas)
 * - Exportaciones de datos
 *
 * Implementa cache-aside pattern con TTL mayor para datos más estáticos.
 *
 * Patrón Hexagonal:
 * Este puerto es DEFINIDO en la capa de aplicación e IMPLEMENTADO
 * por el servicio de dominio.
 */
public interface ListAllProductAttributeValueUseCase {

    /**
     * Obtiene todos los valores de atributos que cumplen el filtro sin paginación.
     * Retorna un DTO optimizado para componentes de selección.
     *
     * @param filter Filtros opcionales (por defecto solo valores activos)
     * @return Uni con lista completa de valores de atributos optimizados
     */
    Uni<List<ProductAttributeValueSelectResponse>> findAll(ProductAttributeValueFilter filter);
}
