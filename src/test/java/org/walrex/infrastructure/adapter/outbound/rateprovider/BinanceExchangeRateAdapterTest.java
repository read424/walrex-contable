package org.walrex.infrastructure.adapter.outbound.rateprovider;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.walrex.application.dto.binance.BinanceP2PResponse;
import org.walrex.domain.model.ExchangeRate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test unitario para BinanceExchangeRateAdapter
 * Prueba el filtrado y transformación de datos de Binance
 */
@ExtendWith(MockitoExtension.class)
class BinanceExchangeRateAdapterTest {

    @Mock
    BinanceP2PRestClient binanceClient;

    @InjectMocks
    BinanceExchangeRateAdapter adapter;

    @BeforeEach
    void setUp() {
        // Setup inicial
    }

    @Test
    void shouldFilterOutNonTradableAds() {
        // Arrange
        BinanceP2PResponse response = createMockResponse();
        BinanceP2PResponse.P2PData tradable = createMockP2PData("ADV001", true, null, "3.75");
        BinanceP2PResponse.P2PData nonTradable = createMockP2PData("ADV002", false, null, "3.76");

        response.setData(List.of(tradable, nonTradable));

        when(binanceClient.searchP2POrders(any())).thenReturn(Uni.createFrom().item(response));

        // Act
        List<ExchangeRate> result = adapter.fetchExchangeRates("USDT", "PEN", "BUY", null, null)
                .await()
                .indefinitely();

        // Assert
        assertEquals(1, result.size(), "Debe filtrar los no tradables");
        assertEquals("ADV001", result.get(0).advNo());
    }

    @Test
    void shouldFilterOutPrivilegedAds() {
        // Arrange
        BinanceP2PResponse response = createMockResponse();
        BinanceP2PResponse.P2PData normal = createMockP2PData("ADV001", true, null, "3.75");
        BinanceP2PResponse.P2PData privileged = createMockP2PData("ADV002", true, "VIP_ONLY", "3.76");

        response.setData(List.of(normal, privileged));

        when(binanceClient.searchP2POrders(any())).thenReturn(Uni.createFrom().item(response));

        // Act
        List<ExchangeRate> result = adapter.fetchExchangeRates("USDT", "PEN", "BUY", null, null)
                .await()
                .indefinitely();

        // Assert
        assertEquals(1, result.size(), "Debe filtrar los privilegiados");
        assertEquals("ADV001", result.get(0).advNo());
    }

    @Test
    void shouldMapFieldsCorrectly() {
        // Arrange
        BinanceP2PResponse response = createMockResponse();
        BinanceP2PResponse.P2PData data = createMockP2PData("ADV123", true, null, "3.75");
        data.getAdv().setMinSingleTransAmount("100");
        data.getAdv().setMaxSingleTransAmount("5000");
        data.getAdv().setPayTimeLimit(15);

        // Agregar métodos de pago
        BinanceP2PResponse.P2PData.TradeMethod method1 = new BinanceP2PResponse.P2PData.TradeMethod();
        method1.setTradeMethodShortName("BCP");
        method1.setTradeMethodName("Banco de Crédito del Perú");
        data.getAdv().setTradeMethods(List.of(method1));

        response.setData(List.of(data));

        when(binanceClient.searchP2POrders(any())).thenReturn(Uni.createFrom().item(response));

        // Act
        List<ExchangeRate> result = adapter.fetchExchangeRates("USDT", "PEN", "BUY", List.of("CreditBankofPeru", "BancoDeCredito", "Interbank", "Plin", "Yape", "ScotiabankPeru"), null)
                .await()
                .indefinitely();

        // Assert
        assertEquals(1, result.size());
        ExchangeRate rate = result.get(0);
        assertEquals("ADV123", rate.advNo());
        assertEquals("USDT", rate.currencyBase());
        assertEquals("PEN", rate.currencyQuote());
        assertEquals("BUY", rate.tradeType());
        assertEquals(new BigDecimal("3.75"), rate.price());
        assertEquals(new BigDecimal("100"), rate.minAmount());
        assertEquals(new BigDecimal("5000"), rate.maxAmount());
        assertEquals(15, rate.payTimeLimit());
        assertEquals(1, rate.paymentMethods().size());
        assertEquals("BCP", rate.paymentMethods().get(0).shortName());
        assertEquals("Banco de Crédito del Perú", rate.paymentMethods().get(0).fullName());
    }

    @Test
    void shouldHandleEmptyResponse() {
        // Arrange
        BinanceP2PResponse response = createMockResponse();
        response.setData(new ArrayList<>());

        when(binanceClient.searchP2POrders(any())).thenReturn(Uni.createFrom().item(response));

        // Act
        List<ExchangeRate> result = adapter.fetchExchangeRates("USDT", "PEN", "BUY", null, null)
                .await()
                .indefinitely();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandleNullResponse() {
        // Arrange
        when(binanceClient.searchP2POrders(any())).thenReturn(Uni.createFrom().nullItem());

        // Act
        List<ExchangeRate> result = adapter.fetchExchangeRates("USDT", "PEN", "BUY", null, null)
                .await()
                .indefinitely();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRecoverFromApiFailure() {
        // Arrange
        when(binanceClient.searchP2POrders(any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("API Error")));

        // Act
        List<ExchangeRate> result = adapter.fetchExchangeRates("USDT", "PEN", "BUY", null, null)
                .await()
                .indefinitely();

        // Assert
        assertTrue(result.isEmpty(), "Debe recuperarse y devolver lista vacía");
    }

    // Helper methods

    private BinanceP2PResponse createMockResponse() {
        BinanceP2PResponse response = new BinanceP2PResponse();
        response.setCode("000000");
        response.setSuccess(true);
        response.setTotal(1);
        return response;
    }

    private BinanceP2PResponse.P2PData createMockP2PData(
            String advNo,
            boolean isTradable,
            String privilegeDesc,
            String price) {

        BinanceP2PResponse.P2PData data = new BinanceP2PResponse.P2PData();

        BinanceP2PResponse.P2PData.Adv adv = new BinanceP2PResponse.P2PData.Adv();
        adv.setAdvNo(advNo);
        adv.setClassify("mass");
        adv.setPrice(price);
        adv.setMinSingleTransAmount("100");
        adv.setMaxSingleTransAmount("10000");
        adv.setPayTimeLimit(15);
        adv.setIsTradable(isTradable);
        adv.setPrivilegeDesc(privilegeDesc);
        adv.setTradeMethods(new ArrayList<>());

        BinanceP2PResponse.P2PData.Advertiser advertiser = new BinanceP2PResponse.P2PData.Advertiser();
        advertiser.setNickName("TestMerchant");

        data.setAdv(adv);
        data.setAdvertiser(advertiser);

        return data;
    }
}
