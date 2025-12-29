package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;

import java.util.List;

/**
 * Port for querying payment methods for Binance P2P
 */
public interface PaymentMethodQueryPort {

    /**
     * Get all active Binance payment method codes for a country_currency
     *
     * @param countryCurrencyId ID from country_currencies table
     * @return Uni with list of Binance payment codes (bank.name_pay_binance values)
     */
    Uni<List<String>> findBinancePaymentCodesByCountryCurrency(Long countryCurrencyId);
}
