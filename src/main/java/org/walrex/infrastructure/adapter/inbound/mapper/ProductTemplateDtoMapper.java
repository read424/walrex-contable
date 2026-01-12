package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.request.CreateProductTemplateRequest;
import org.walrex.application.dto.request.UpdateProductTemplateRequest;
import org.walrex.application.dto.response.ProductTemplateResponse;
import org.walrex.application.dto.response.ProductTemplateSelectResponse;
import org.walrex.domain.model.ProductTemplate;
import org.walrex.domain.model.ProductType;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductTemplateEntity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Mapper entre el modelo de dominio ProductTemplate y los DTOs de entrada/salida.
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de compilación.
 *
 * IMPORTANTE: Este mapper tiene dos responsabilidades principales:
 * 1. Convertir DTOs de request → ProductTemplate (dominio)
 * 2. Convertir ProductTemplateEntity → ProductTemplateResponse (con info de entidades relacionadas)
 *
 * Para las respuestas, se usa toResponseFromEntity() que extrae:
 * - categoryName desde entity.category.name
 * - brandName desde entity.brand.name
 * - uomCode desde entity.uom.codeUom
 * - currencyCode desde entity.currency.alphabeticCode
 *
 * Esto requiere que la entidad haya sido cargada con JOIN FETCH.
 *
 * Configuración:
 * - componentModel = "cdi": Integración con CDI de Quarkus (permite @Inject)
 * - unmappedTargetPolicy = IGNORE: Ignora campos no mapeados
 * - injectionStrategy = CONSTRUCTOR: Usa inyección por constructor (mejor para inmutabilidad)
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ProductTemplateDtoMapper {

    // ==================== Request DTOs → Domain ====================

    /**
     * Convierte CreateProductTemplateRequest a ProductTemplate (dominio).
     *
     * @param request DTO de creación
     * @return ProductTemplate modelo de dominio
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    ProductTemplate toDomain(CreateProductTemplateRequest request);

    /**
     * Convierte UpdateProductTemplateRequest a ProductTemplate (dominio).
     *
     * @param request DTO de actualización
     * @return ProductTemplate modelo de dominio
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    ProductTemplate toDomain(UpdateProductTemplateRequest request);

    // ==================== Domain → Response DTOs ====================

    /**
     * Convierte el modelo de dominio a DTO de respuesta.
     *
     * NOTA: Este método NO puede incluir categoryName, brandName, uomCode, currencyCode
     * porque el modelo de dominio solo tiene los IDs.
     * Use toResponseFromEntity si necesita esos campos.
     *
     * @param productTemplate Modelo de dominio
     * @return ProductTemplateResponse DTO de respuesta (campos de entidades relacionadas serán null)
     */
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "brandName", ignore = true)
    @Mapping(target = "uomCode", ignore = true)
    @Mapping(target = "currencyCode", ignore = true)
    ProductTemplateResponse toResponse(ProductTemplate productTemplate);

    /**
     * Convierte la entidad de persistencia a DTO de respuesta.
     *
     * Este método extrae categoryName, brandName, uomCode, currencyCode de las entidades relacionadas
     * si fueron cargadas con JOIN FETCH. Si alguna relación es null, esos campos serán null.
     *
     * @param entity Entidad de persistencia (idealmente con category, brand, uom, currency cargados)
     * @return ProductTemplateResponse DTO de respuesta con información completa
     */
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "brand.name", target = "brandName")
    @Mapping(source = "uom.codeUom", target = "uomCode")
    @Mapping(source = "currency.alphabeticCode", target = "currencyCode")
    ProductTemplateResponse toResponseFromEntity(ProductTemplateEntity entity);

    /**
     * Convierte una lista de ProductTemplateEntity a lista de ProductTemplateResponse.
     *
     * @param entities Lista de entidades de persistencia
     * @return Lista de DTOs de respuesta
     */
    List<ProductTemplateResponse> toResponseListFromEntities(List<ProductTemplateEntity> entities);

    // ==================== Domain → Select Response DTOs ====================

    /**
     * Convierte el modelo de dominio a DTO de respuesta optimizado para selects.
     *
     * Solo incluye campos esenciales: id, name, internalReference, type, salePrice
     *
     * @param productTemplate Modelo de dominio
     * @return ProductTemplateSelectResponse DTO optimizado para selects
     */
    ProductTemplateSelectResponse toSelectResponse(ProductTemplate productTemplate);

    /**
     * Convierte una lista de ProductTemplate a lista de ProductTemplateSelectResponse.
     *
     * @param productTemplates Lista de modelos de dominio
     * @return Lista de DTOs optimizados para selects
     */
    List<ProductTemplateSelectResponse> toSelectResponseList(List<ProductTemplate> productTemplates);

    // ==================== Map (SQL Nativo) → Response DTO ====================

    /**
     * Convierte resultado de query SQL nativa (Map) a ProductTemplateResponse.
     *
     * Usado cuando se ejecutan queries SQL nativas que retornan Map<String, Object>.
     *
     * @param row Map con los datos de la fila SQL
     * @return ProductTemplateResponse DTO de respuesta
     */
    default ProductTemplateResponse toResponseFromMap(Map<String, Object> row) {
        return new ProductTemplateResponse(
                (Integer) row.get("id"),
                (String) row.get("name"),
                (String) row.get("internalReference"),
                ProductType.fromValue((String) row.get("type")),
                (Integer) row.get("categoryId"),
                (String) row.get("categoryName"),
                (Integer) row.get("brandId"),
                (String) row.get("brandName"),
                (Integer) row.get("uomId"),
                (String) row.get("uomCode"),
                (Integer) row.get("currencyId"),
                (String) row.get("currencyCode"),
                (BigDecimal) row.get("salePrice"),
                (BigDecimal) row.get("cost"),
                (Boolean) row.get("isIgvExempt"),
                (BigDecimal) row.get("taxRate"),
                (BigDecimal) row.get("weight"),
                (BigDecimal) row.get("volume"),
                (Boolean) row.get("trackInventory"),
                (Boolean) row.get("useSerialNumbers"),
                (BigDecimal) row.get("minimumStock"),
                (BigDecimal) row.get("maximumStock"),
                (BigDecimal) row.get("reorderPoint"),
                (Integer) row.get("leadTime"),
                (String) row.get("image"),
                (String) row.get("description"),
                (String) row.get("descriptionSale"),
                (String) row.get("barcode"),
                (String) row.get("notes"),
                (Boolean) row.get("canBeSold"),
                (Boolean) row.get("canBePurchased"),
                (Boolean) row.get("allowsPriceEdit"),
                (Boolean) row.get("hasVariants"),
                (String) row.get("status"),
                convertToOffsetDateTime(row.get("createdAt")),
                convertToOffsetDateTime(row.get("updatedAt"))
        );
    }

    /**
     * Convierte lista de Maps a lista de ProductTemplateResponse.
     *
     * @param rows Lista de Maps con datos SQL
     * @return Lista de DTOs de respuesta
     */
    default List<ProductTemplateResponse> toResponseListFromMaps(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(this::toResponseFromMap)
                .toList();
    }

    /**
     * Convierte Timestamp/LocalDateTime a OffsetDateTime.
     * Maneja los diferentes tipos que puede retornar PostgreSQL/JDBC.
     */
    default OffsetDateTime convertToOffsetDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime) {
            return (OffsetDateTime) value;
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).atOffset(ZoneOffset.UTC);
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().atOffset(ZoneOffset.UTC);
        }
        return null;
    }
}
