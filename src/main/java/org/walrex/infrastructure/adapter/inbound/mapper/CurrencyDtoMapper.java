package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.request.CreateCurrencyRequest;
import org.walrex.application.dto.response.CurrencyResponse;
import org.walrex.domain.model.Currency;

import java.util.List;

/**
 * Mapper entre el modelo de dominio Currency y los DTOs de la capa de aplicación.
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de compilación.
 *
 * Configuración:
 * - componentModel = "cdi": Integración con CDI de Quarkus (permite @Inject)
 * - unmappedTargetPolicy = IGNORE: Ignora campos no mapeados
 * - injectionStrategy = CONSTRUCTOR: Usa inyección por constructor (mejor para inmutabilidad)
 *
 * Responsabilidades:
 * - Convertir Currency (dominio) → CurrencyResponse (DTO salida)
 * - Convertir CreateCurrencyRequest (DTO entrada) → Currency (dominio)
 * - Manejar conversiones de tipos (Integer ↔ String para numericCode)
 * - Normalizar datos (status ↔ active)
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface CurrencyDtoMapper {

    /**
     * Convierte el modelo de dominio a DTO de respuesta.
     *
     * Mapeos especiales:
     * - numericCode: Integer → String (usa formatNumericCode)
     * - status → active: Boolean → boolean (usa mapStatusToActive)
     * - deletedAt: Se ignora (no se expone al exterior)
     *
     * @param currency Modelo de dominio
     * @return CurrencyResponse DTO de respuesta
     */
    @Mapping(target = "numericCode", source = "numericCode", qualifiedByName = "formatNumericCode")
    CurrencyResponse toResponse(Currency currency);

    /**
     * Convierte un DTO de creación a modelo de dominio.
     *
     * Mapeos especiales:
     * - numericCode: String → Integer (usa parseNumericCode)
     * - status: Se establece como true (expresión constante)
     * - createdAt, updatedAt: Se establecen con timestamp actual
     * - id, deletedAt: Se ignoran (aún no existen para nuevas monedas)
     *
     * @param request DTO de creación
     * @return Currency modelo de dominio
     */
    @Mapping(target = "numericCode", source = "numericCode", qualifiedByName = "parseNumericCode")
    @Mapping(target = "status", constant = "true")
    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC))")
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC))")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Currency toDomain(CreateCurrencyRequest request);

    /**
     * Convierte una lista de Currency a lista de CurrencyResponse.
     *
     * @param currencies Lista de modelos de dominio
     * @return Lista de DTOs de respuesta
     */
    List<CurrencyResponse> toResponseList(List<Currency> currencies);

    // ==================== Métodos de Conversión Personalizados ====================

    /**
     * Formatea el código numérico como String con padding de ceros.
     *
     * Ejemplos:
     * - 840 → "840"
     * - 32 → "032"
     * - null → null
     *
     * @param numericCode Código numérico como Integer
     * @return Código formateado como String
     */
    @Named("formatNumericCode")
    default String formatNumericCode(Integer numericCode) {
        if (numericCode == null) {
            return null;
        }
        return String.format("%03d", numericCode);
    }

    /**
     * Parsea el código numérico de String a Integer.
     *
     * Ejemplos:
     * - "840" → 840
     * - "032" → 32
     * - null → null
     *
     * @param numericCode Código numérico como String
     * @return Código como Integer
     * @throws NumberFormatException si el string no es un número válido
     */
    @Named("parseNumericCode")
    default Integer parseNumericCode(String numericCode) {
        if (numericCode == null || numericCode.isBlank()) {
            return null;
        }
        return Integer.parseInt(numericCode.trim());
    }
}