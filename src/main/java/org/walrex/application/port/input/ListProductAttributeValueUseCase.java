package org.walrex.application.port.input;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductAttributeValueFilter;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductAttributeValueResponse;

/**
 * Puerto de entrada (Input Port) para listar valores de atributos de producto con paginación.
 *
 * Este caso de uso encapsula la lógica para consultar valores de atributos:
 * 1. Aplicar filtros opcionales (search, attributeId, active, etc.)
 * 2. Aplicar paginación y ordenamiento
 * 3. Intentar obtener del caché
 * 4. Si no está en caché, consultar DB y cachear resultado
 * 5. Retornar respuesta paginada
 *
 * Implementa cache-aside pattern para optimizar rendimiento.
 *
 * Patrón Hexagonal:
 * Este puerto es DEFINIDO en la capa de aplicación e IMPLEMENTADO
 * por el servicio de dominio.
 */
public interface ListProductAttributeValueUseCase {

    /**
     * Lista valores de atributos de producto con paginación, filtros y ordenamiento.
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales (search, attributeId, active, etc.)
     * @return Uni con respuesta paginada de valores de atributos
     */
    Uni<PagedResponse<ProductAttributeValueResponse>> execute(
            PageRequest pageRequest,
            ProductAttributeValueFilter filter
    );

    /**
     * Stream reactivo de todos los valores de atributos activos.
     *
     * @return Multi que emite cada valor de atributo individualmente
     */
    Multi<ProductAttributeValueResponse> streamAll();

    /**
     * Stream reactivo de valores de atributos con filtros aplicados.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada valor de atributo que cumple los filtros
     */
    Multi<ProductAttributeValueResponse> streamWithFilter(ProductAttributeValueFilter filter);
}
