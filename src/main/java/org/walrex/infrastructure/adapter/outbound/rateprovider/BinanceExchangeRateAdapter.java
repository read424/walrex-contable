package org.walrex.infrastructure.adapter.outbound.rateprovider;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.walrex.application.dto.binance.BinanceP2PRequest;
import org.walrex.application.dto.binance.BinanceP2PResponse;
import org.walrex.application.port.output.ExchangeRateProviderPort;
import org.walrex.domain.model.ExchangeRate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adaptador que implementa el puerto ExchangeRateProviderPort
 * usando Binance P2P como fuente de datos
 */
@Slf4j
@ApplicationScoped
@RegisterForReflection
public class BinanceExchangeRateAdapter implements ExchangeRateProviderPort {

    @RestClient
    BinanceP2PRestClient binanceClient;

    @Override
    public Uni<List<ExchangeRate>> fetchExchangeRates(
            String asset,
            String fiat,
            String tradeType,
            List<String> payTypes,
            BigDecimal transAmount) {

        log.info("Fetching exchange rates from Binance P2P: asset={}, fiat={}, tradeType={}",
                asset, fiat, tradeType);

        BinanceP2PRequest request = BinanceP2PRequest.builder()
                .additionalKycVerifyFilter(0)
                .asset(asset)
                .classifies(List.of("mass", "profession", "fiat_trade"))
                .countries(Collections.emptyList())
                .fiat(fiat)
                .filterType("all")
                .page(1)
                .payTypes(payTypes != null ? payTypes : Collections.emptyList())
                .periods(Collections.emptyList())
                .proMerchantAds(false)
                .publisherType(null)
                .rows(10)
                .shieldMerchantAds(false)
                .tradeType(tradeType)
                .transAmount(transAmount)
                .build();

        // Headers para evitar compresión gzip que causa problemas de parsing
        return binanceClient.searchP2POrders(
                "identity",  // No gzip, solo identidad
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36",  // User agent
                "application/json",  // Accept
                request
        ).map(response -> {
                    if (response == null || response.getData() == null) {
                        log.warn("Empty response from Binance P2P API");
                        return Collections.<ExchangeRate>emptyList();
                    }

                    // Filtrar y transformar los datos
                    List<ExchangeRate> rates = response.getData().stream()
                            .filter(this::isValidP2PData)
                            .map(data -> mapToExchangeRate(data, asset, fiat, tradeType))
                            .collect(Collectors.toList());

                    log.info("Fetched {} exchange rates from Binance P2P", rates.size());
                    return rates;
                })
                .onFailure().invoke(error ->
                        log.error("Error fetching rates from Binance P2P: {}", error.getMessage(), error)
                )
                .onFailure().recoverWithItem(Collections.emptyList());
    }

    @Override
    public Uni<List<ExchangeRate>> fetchExchangeRates(
            String asset,
            String fiat,
            String tradeType,
            List<String> payTypes,
            BigDecimal transAmount,
            BigDecimal transCryptoAmount) {

        log.info("Fetching exchange rates from Binance P2P: asset={}, fiat={}, tradeType={}, transCryptoAmount={}",
                asset, fiat, tradeType, transCryptoAmount);

        BinanceP2PRequest request = BinanceP2PRequest.builder()
                .additionalKycVerifyFilter(0)
                .asset(asset)
                .classifies(List.of("mass", "profession", "fiat_trade"))
                .countries(Collections.emptyList())
                .fiat(fiat)
                .filterType("all")
                .page(1)
                .payTypes(payTypes != null ? payTypes : Collections.emptyList())
                .periods(Collections.emptyList())
                .proMerchantAds(false)
                .publisherType(null)
                .rows(10)
                .shieldMerchantAds(false)
                .tradeType(tradeType)
                .transAmount(transAmount)
                .transCryptoAmount(transCryptoAmount)
                .build();

        // Headers para evitar compresión gzip que causa problemas de parsing
        return binanceClient.searchP2POrders(
                "identity",  // No gzip, solo identidad
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36",  // User agent
                "application/json",  // Accept
                request
        ).map(response -> {
                    if (response == null || response.getData() == null) {
                        log.warn("Empty response from Binance P2P API");
                        return Collections.<ExchangeRate>emptyList();
                    }

                    // Filtrar y transformar los datos
                    List<ExchangeRate> rates = response.getData().stream()
                            .filter(this::isValidP2PData)
                            .map(data -> mapToExchangeRate(data, asset, fiat, tradeType))
                            .collect(Collectors.toList());

                    log.info("Fetched {} exchange rates from Binance P2P", rates.size());
                    return rates;
                })
                .onFailure().invoke(error ->
                        log.error("Error fetching rates from Binance P2P: {}", error.getMessage(), error)
                )
                .onFailure().recoverWithItem(Collections.emptyList());
    }

    /**
     * Valida que el registro de Binance sea válido según las reglas de negocio:
     * - isTradable debe ser true
     * - privilegeDesc debe ser null
     */
    private boolean isValidP2PData(BinanceP2PResponse.P2PData data) {
        if (data == null || data.getAdv() == null) {
            return false;
        }

        BinanceP2PResponse.P2PData.Adv adv = data.getAdv();

        // Filtrar: isTradable debe ser true
        if (!Boolean.TRUE.equals(adv.getIsTradable())) {
            log.debug("Filtering out non-tradable ad: {}", adv.getAdvNo());
            return false;
        }

        // Filtrar: privilegeDesc debe ser null
        if (adv.getPrivilegeDesc() != null) {
            log.debug("Filtering out privileged ad: {}", adv.getAdvNo());
            return false;
        }

        return true;
    }

    /**
     * Mapea los datos de Binance al modelo de dominio ExchangeRate
     */
    private ExchangeRate mapToExchangeRate(
            BinanceP2PResponse.P2PData data,
            String asset,
            String fiat,
            String tradeType) {

        BinanceP2PResponse.P2PData.Adv adv = data.getAdv();
        BinanceP2PResponse.P2PData.Advertiser advertiser = data.getAdvertiser();

        List<ExchangeRate.PaymentMethod> paymentMethods = adv.getTradeMethods() != null
                ? adv.getTradeMethods().stream()
                .map(method -> new ExchangeRate.PaymentMethod(
                        method.getTradeMethodShortName(),
                        method.getTradeMethodName()
                ))
                .collect(Collectors.toList())
                : Collections.emptyList();

        return new ExchangeRate(
                adv.getAdvNo(),
                asset,
                fiat,
                tradeType,
                new BigDecimal(adv.getPrice()),
                new BigDecimal(adv.getMinSingleTransAmount()),
                new BigDecimal(adv.getMaxSingleTransAmount()),
                adv.getPayTimeLimit(),
                paymentMethods,
                advertiser != null ? advertiser.getNickName() : null,
                adv.getClassify(),
                LocalDateTime.now()
        );
    }
}
