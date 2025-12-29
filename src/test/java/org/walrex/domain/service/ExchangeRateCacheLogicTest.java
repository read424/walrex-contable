package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.walrex.application.port.output.ExchangeRateCachePort;
import org.walrex.application.port.output.ExchangeRateProviderPort;
import org.walrex.application.port.output.PaymentMethodQueryPort;
import org.walrex.application.port.output.PriceExchangeOutputPort;
import org.walrex.application.port.output.RemittanceRouteOutputPort;
import org.walrex.domain.model.ExchangeRateCache;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para la lógica de caché del ExchangeRateService.
 *
 * Verifica:
 * - Cálculo de diferencia porcentual
 * - Verificación de threshold ±0.5%
 * - Integración con Redis caché
 */
@ExtendWith(MockitoExtension.class)
class ExchangeRateCacheLogicTest {

    @Mock
    ExchangeRateProviderPort exchangeRateProvider;

    @Mock
    PriceExchangeOutputPort priceExchangePort;

    @Mock
    RemittanceRouteOutputPort remittanceRoutePort;

    @Mock
    PaymentMethodQueryPort paymentMethodQueryPort;

    @Mock
    ExchangeRateCachePort cachePort;

    @InjectMocks
    ExchangeRateService service;

    @BeforeEach
    void setUp() {
        // Setup común para tests
    }

    @Test
    void shouldCalculatePositivePercentageChange() {
        // Given
        BigDecimal newRate = BigDecimal.valueOf(9.56);
        BigDecimal cachedRate = BigDecimal.valueOf(9.50);

        // When - Usar reflexión para acceder al método privado
        BigDecimal change = invokeCalculatePercentageChange(newRate, cachedRate);

        // Then
        // ((9.56 - 9.50) / 9.50) * 100 = 0.63158%
        assertTrue(change.compareTo(BigDecimal.valueOf(0.63)) > 0);
        assertTrue(change.compareTo(BigDecimal.valueOf(0.64)) < 0);
    }

    @Test
    void shouldCalculateNegativePercentageChange() {
        // Given
        BigDecimal newRate = BigDecimal.valueOf(9.42);
        BigDecimal cachedRate = BigDecimal.valueOf(9.50);

        // When
        BigDecimal change = invokeCalculatePercentageChange(newRate, cachedRate);

        // Then
        // ((9.42 - 9.50) / 9.50) * 100 = -0.84211%
        assertTrue(change.compareTo(BigDecimal.valueOf(-0.85)) > 0);
        assertTrue(change.compareTo(BigDecimal.valueOf(-0.84)) < 0);
    }

    @Test
    void shouldDetectChangeWithinThreshold() {
        // Given - Cambio de 0.2% (dentro de ±0.5%)
        BigDecimal percentageChange = BigDecimal.valueOf(0.2);

        // When
        boolean exceeds = invokeExceedsThreshold(percentageChange);

        // Then
        assertFalse(exceeds, "0.2% should be within ±0.5% threshold");
    }

    @Test
    void shouldDetectChangeExceedingThreshold() {
        // Given - Cambio de 0.6% (fuera de ±0.5%)
        BigDecimal percentageChange = BigDecimal.valueOf(0.6);

        // When
        boolean exceeds = invokeExceedsThreshold(percentageChange);

        // Then
        assertTrue(exceeds, "0.6% should exceed ±0.5% threshold");
    }

    @Test
    void shouldDetectNegativeChangeExceedingThreshold() {
        // Given - Cambio de -0.8% (fuera de ±0.5%)
        BigDecimal percentageChange = BigDecimal.valueOf(-0.8);

        // When
        boolean exceeds = invokeExceedsThreshold(percentageChange);

        // Then
        assertTrue(exceeds, "-0.8% should exceed ±0.5% threshold");
    }

    @Test
    void shouldDetectChangeExactlyAtThreshold() {
        // Given - Cambio exactamente en el límite (0.5%)
        BigDecimal percentageChange = BigDecimal.valueOf(0.5);

        // When
        boolean exceeds = invokeExceedsThreshold(percentageChange);

        // Then
        assertFalse(exceeds, "0.5% should be within threshold (not exceeding)");
    }

    @Test
    void shouldHandleZeroCachedRate() {
        // Given
        BigDecimal newRate = BigDecimal.valueOf(9.50);
        BigDecimal cachedRate = BigDecimal.ZERO;

        // When
        BigDecimal change = invokeCalculatePercentageChange(newRate, cachedRate);

        // Then
        assertEquals(0, change.compareTo(BigDecimal.valueOf(100)),
                "Should return 100% change when cached rate is zero");
    }

    /**
     * Nota: Los tests de integración completos (con llamadas a saveRateWithCache)
     * no se incluyen porque el método es privado. Los tests de métodos públicos
     * en ExchangeRateServiceTest cubren la integración completa.
     */

    /**
     * Helper methods para invocar métodos privados usando reflexión
     */
    private BigDecimal invokeCalculatePercentageChange(BigDecimal newRate, BigDecimal cachedRate) {
        try {
            var method = ExchangeRateService.class.getDeclaredMethod(
                    "calculatePercentageChange", BigDecimal.class, BigDecimal.class);
            method.setAccessible(true);
            return (BigDecimal) method.invoke(service, newRate, cachedRate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke calculatePercentageChange", e);
        }
    }

    private boolean invokeExceedsThreshold(BigDecimal percentageChange) {
        try {
            var method = ExchangeRateService.class.getDeclaredMethod(
                    "exceedsThreshold", BigDecimal.class);
            method.setAccessible(true);
            return (boolean) method.invoke(service, percentageChange);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke exceedsThreshold", e);
        }
    }
}
