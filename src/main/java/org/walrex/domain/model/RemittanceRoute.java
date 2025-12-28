package org.walrex.domain.model;

/**
 * Modelo de dominio que representa un par de monedas configurado para remesas
 * Define qué tasas de cambio se deben consultar
 *
 * @param currencyFromId ID de la moneda origen en la tabla currencies
 * @param currencyFromCode Código ISO3 de la moneda origen (ej: PEN, VES)
 * @param currencyToId ID de la moneda destino en la tabla currencies
 * @param currencyToCode Código ISO3 de la moneda destino (ej: VES, PEN)
 * @param intermediaryAssetId ID del activo intermediario en la tabla currencies
 * @param intermediaryAsset Activo intermediario usado en el exchange (ej: USDT)
 */
public record RemittanceRoute(
        Integer currencyFromId,
        String currencyFromCode,
        Integer currencyToId,
        String currencyToCode,
        Integer intermediaryAssetId,
        String intermediaryAsset
) {
}
