package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.walrex.application.port.output.ExchangeRateProviderPort;
import org.walrex.domain.model.ExchangeRate;
import org.walrex.domain.model.RemittanceRoute;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * Test unitario puro para ExchangeRateService sin levantar contexto de Quarkus
 */
@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    ExchangeRateProviderPort exchangeRateProvider;

    @Mock
    org.walrex.application.port.output.PriceExchangeOutputPort priceExchangePort;

    @Mock
    org.walrex.application.port.output.RemittanceRouteOutputPort remittanceRoutePort;

    @InjectMocks
    ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        // Setup para el mock de persistencia (lenient porque no todos los tests lo usan)
        Mockito.lenient().when(priceExchangePort.upsertRate(anyInt(), anyInt(), any(), any()))
                .thenReturn(Uni.createFrom().item(1));
    }

    @Test
    void shouldFetchExchangeRatesSuccessfully() {
        // Arrange - Configurar rutas de remesas
        List<RemittanceRoute> routes = List.of(
                new RemittanceRoute(1, "PEN",2, "VES", "USDT"),
                new RemittanceRoute(2, "VES", 1,"PEN", "USDT")
        );

        Mockito.when(remittanceRoutePort.findAllActiveRoutes())
                .thenReturn(Uni.createFrom().item(routes));

        // Mock tasas para PEN
        List<ExchangeRate> mockPenRates = List.of(
                createMockRate("ADV001", "USDT", "PEN", "BUY", "3.75"),
                createMockRate("ADV002", "USDT", "PEN", "BUY", "3.76")
        );

        // Mock tasas para VES
        List<ExchangeRate> mockVesRates = List.of(
                createMockRate("ADV003", "USDT", "VES", "BUY", "36.50")
        );

        Mockito.when(exchangeRateProvider.fetchExchangeRates(eq("USDT"), eq("PEN"), eq("BUY"), any(), any()))
                .thenReturn(Uni.createFrom().item(mockPenRates));

        Mockito.when(exchangeRateProvider.fetchExchangeRates(eq("USDT"), eq("PEN"), eq("SELL"), any(), any()))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        Mockito.when(exchangeRateProvider.fetchExchangeRates(eq("USDT"), eq("VES"), eq("BUY"), any(), any()))
                .thenReturn(Uni.createFrom().item(mockVesRates));

        Mockito.when(exchangeRateProvider.fetchExchangeRates(eq("USDT"), eq("VES"), eq("SELL"), any(), any()))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        // Act
        ExchangeRateService.ExchangeRateUpdate result = exchangeRateService
                .updateExchangeRates()
                .await()
                .indefinitely();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.ratesByPair().size()); // Dos pares: USDT/PEN y USDT/VES

        // Verificar tasas de PEN
        ExchangeRateService.RouteRates penRates = result.ratesByPair().get("USDT/PEN");
        assertNotNull(penRates);
        assertEquals(2, penRates.buyRates().size());
        assertEquals(0, penRates.sellRates().size());

        // Verificar tasas de VES
        ExchangeRateService.RouteRates vesRates = result.ratesByPair().get("USDT/VES");
        assertNotNull(vesRates);
        assertEquals(1, vesRates.buyRates().size());
        assertEquals(0, vesRates.sellRates().size());

        // Verify que se llamó a cada endpoint
        Mockito.verify(exchangeRateProvider).fetchExchangeRates("USDT", "PEN", "BUY", null, null);
        Mockito.verify(exchangeRateProvider).fetchExchangeRates("USDT", "PEN", "SELL", null, null);
        Mockito.verify(exchangeRateProvider).fetchExchangeRates("USDT", "VES", "BUY", null, null);
        Mockito.verify(exchangeRateProvider).fetchExchangeRates("USDT", "VES", "SELL", null, null);
    }

    @Test
    void shouldHandleProviderFailureGracefully() {
        // Arrange - Configurar rutas de remesas
        List<RemittanceRoute> routes = List.of(
                new RemittanceRoute(1,"PEN",2, "VES", "USDT")
        );

        Mockito.when(remittanceRoutePort.findAllActiveRoutes())
                .thenReturn(Uni.createFrom().item(routes));

        // Simular que el proveedor falla
        Mockito.when(exchangeRateProvider.fetchExchangeRates(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Provider error")));

        // Act
        ExchangeRateService.ExchangeRateUpdate result = exchangeRateService
                .updateExchangeRates()
                .await()
                .indefinitely();

        // Assert - Debe recuperarse y devolver listas vacías
        assertNotNull(result);
        assertEquals(1, result.ratesByPair().size()); // Tiene un par aunque haya fallado

        // El par se agrupa por intermediaryAsset/currencyQuoteCode
        ExchangeRateService.RouteRates vesRates = result.ratesByPair().get("USDT/VES");
        assertNotNull(vesRates);
        assertTrue(vesRates.buyRates().isEmpty());
        assertTrue(vesRates.sellRates().isEmpty());
    }

    @Test
    void shouldHandleNoRoutesConfigured() {
        // Arrange - No hay rutas configuradas
        Mockito.when(remittanceRoutePort.findAllActiveRoutes())
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        // Act
        ExchangeRateService.ExchangeRateUpdate result = exchangeRateService
                .updateExchangeRates()
                .await()
                .indefinitely();

        // Assert - Debe devolver un mapa vacío
        assertNotNull(result);
        assertTrue(result.ratesByPair().isEmpty());

        // Verify que no se llamó al proveedor
        Mockito.verify(exchangeRateProvider, Mockito.never())
                .fetchExchangeRates(anyString(), anyString(), anyString(), any(), any());
    }

    private ExchangeRate createMockRate(String advNo, String base, String quote, String type, String price) {
        return new ExchangeRate(
                advNo,
                base,
                quote,
                type,
                new BigDecimal(price),
                new BigDecimal("100"),
                new BigDecimal("10000"),
                15,
                Collections.emptyList(),
                "TestMerchant",
                "mass",
                LocalDateTime.now()
        );
    }
}
