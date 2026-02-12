package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.walrex.application.port.output.ExchangeRateProviderPort;
import org.walrex.application.port.output.PaymentMethodQueryPort;
import org.walrex.application.port.output.RemittanceRouteOutputPort;
import org.walrex.domain.model.ExchangeCalculation;
import org.walrex.domain.model.ExchangeRate;
import org.walrex.domain.model.ExchangeRateRouteInfo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Test unitario puro para ExchangeRateCalculationService sin levantar contexto de Quarkus.
 */
@ExtendWith(MockitoExtension.class)
class ExchangeRateCalculationServiceTest {

    @Mock
    RemittanceRouteOutputPort remittanceRoutePort;

    @Mock
    PaymentMethodQueryPort paymentMethodPort;

    @Mock
    ExchangeRateProviderPort exchangeRateProvider;

    @InjectMocks
    ExchangeRateCalculationService service;

    @Test
    void shouldCalculateExchangeRateSuccessfully() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(100.00);
        String baseCountry = "PE";
        String baseCurrency = "PEN";
        String quoteCountry = "VE";
        String quoteCurrency = "VES";
        BigDecimal margin = BigDecimal.valueOf(5.0);

        ExchangeRateRouteInfo route = ExchangeRateRouteInfo.builder()
                .countryCurrencyFromId(1L)
                .currencyFromId(1)
                .countryFromCode("PE")
                .currencyFromCode("PEN")
                .countryCurrencyToId(2L)
                .currencyToId(2)
                .countryToCode("VE")
                .currencyToCode("VES")
                .intermediaryAsset("USDT")
                .build();

        List<ExchangeRateRouteInfo> routes = List.of(route);

        // Mock payment methods
        List<String> penPaymentMethods = List.of("Yape", "Plin", "BancoDeCredito");
        List<String> vesPaymentMethods = List.of("PagoMovil", "Banesco");

        // Mock buy rates (BUY USDT/PEN)
        List<ExchangeRate> buyRates = List.of(
                createMockRate("3.80"),
                createMockRate("3.81"),
                createMockRate("3.79"),
                createMockRate("3.82"),
                createMockRate("3.78")
        );

        // Mock sell rates (SELL USDT/VES)
        List<ExchangeRate> sellRates = List.of(
                createMockRate("36.50"),
                createMockRate("36.45"),
                createMockRate("36.55"),
                createMockRate("36.48"),
                createMockRate("36.52")
        );

        when(remittanceRoutePort.findAllActiveExchangeRateRoutes()).thenReturn(Uni.createFrom().item(routes));
        when(paymentMethodPort.findBinancePaymentCodesByCountryCurrency(1L))
                .thenReturn(Uni.createFrom().item(penPaymentMethods));
        when(paymentMethodPort.findBinancePaymentCodesByCountryCurrency(2L))
                .thenReturn(Uni.createFrom().item(vesPaymentMethods));
        when(exchangeRateProvider.fetchExchangeRates(
                eq("USDT"), eq("PEN"), eq("BUY"), eq(penPaymentMethods), isNull()))
                .thenReturn(Uni.createFrom().item(buyRates));
        when(exchangeRateProvider.fetchExchangeRates(
                eq("USDT"), eq("VES"), eq("SELL"), eq(vesPaymentMethods), isNull()))
                .thenReturn(Uni.createFrom().item(sellRates));

        // Act
        ExchangeCalculation result = service
                .calculateExchangeRate(amount, baseCountry, baseCurrency, quoteCountry, quoteCurrency, margin)
                .await()
                .indefinitely();

        // Assert
        assertNotNull(result);
        assertEquals(amount, result.amount());
        assertEquals(baseCurrency, result.baseCurrency());
        assertEquals(quoteCurrency, result.quoteCurrency());
        assertEquals(margin, result.marginApplied());

        // Verificar que los precios promedio están calculados correctamente
        // Promedio buy: (3.80 + 3.81 + 3.79 + 3.82 + 3.78) / 5 = 3.80
        assertEquals(0, result.averageBuyPrice().compareTo(BigDecimal.valueOf(3.80)));

        // Promedio sell: (36.50 + 36.45 + 36.55 + 36.48 + 36.52) / 5 = 36.50
        assertEquals(0, result.averageSellPrice().compareTo(BigDecimal.valueOf(36.50)));

        // Verificar que la tasa y el monto convertido son razonables
        assertTrue(result.rate().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.exchangeRate().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.convertedAmount().compareTo(BigDecimal.ZERO) > 0);

        // La tasa con margen debe ser menor que la tasa sin margen (dividimos para aplicar margen)
        assertTrue(result.exchangeRate().compareTo(result.rate()) < 0);
    }

    @Test
    void shouldThrowExceptionWhenRouteNotFound() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(100.00);
        String baseCountry = "PEN";
        String baseCurrency = "USD";
        String quoteCountry = "VES";
        String quoteCurrency = "VES";
        BigDecimal margin = BigDecimal.valueOf(5.0);

        when(remittanceRoutePort.findAllActiveExchangeRateRoutes())
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.calculateExchangeRate(amount, baseCountry, baseCurrency, quoteCountry, quoteCurrency, margin)
                        .await()
                        .indefinitely()
        );

        assertTrue(exception.getMessage().contains("No active remittance route found"));
        assertTrue(exception.getMessage().contains("PEN (USD) -> VES (VES)"));
    }

    @Test
    void shouldThrowExceptionWhenNoExchangeRatesAvailable() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(100.00);
        String baseCountry = "PE";
        String baseCurrency = "PEN";
        String quoteCountry = "VES";
        String quoteCurrency = "VES";
        BigDecimal margin = BigDecimal.valueOf(5.0);

        when(remittanceRoutePort.findAllActiveExchangeRateRoutes())
                .thenReturn(Uni.createFrom().item(List.of(ExchangeRateRouteInfo.builder()
                        .countryCurrencyFromId(1L)
                        .currencyFromId(1)
                        .countryFromCode("PE")
                        .currencyFromCode("PEN")
                        .countryCurrencyToId(2L)
                        .currencyToId(2)
                        .countryToCode("VES")
                        .currencyToCode("VES")
                        .intermediaryAsset("USDT")
                        .build())));
        when(paymentMethodPort.findBinancePaymentCodesByCountryCurrency(anyLong()))
                .thenReturn(Uni.createFrom().item(List.of("Yape")));
        when(exchangeRateProvider.fetchExchangeRates(any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                service.calculateExchangeRate(amount, baseCountry, baseCurrency, quoteCountry, quoteCurrency, margin)
                        .await()
                        .indefinitely()
        );

        assertTrue(exception.getMessage().contains("No exchange rates available"));
    }

    /**
     * Helper para crear un ExchangeRate de prueba.
     */
    private ExchangeRate createMockRate(String price) {
        return new ExchangeRate(
                "ADV001",
                "USDT",
                "PEN",
                "BUY",
                new BigDecimal(price),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10000),
                15,
                Collections.emptyList(),
                "TestMerchant",
                "mass",
                LocalDateTime.now()
        );
    }
}
