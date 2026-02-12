package org.walrex.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.walrex.application.dto.request.GenerateQrRequest;
import org.walrex.application.dto.response.DecodeQrResponse;
import org.walrex.application.port.output.MerchantQrOutputPort;
import org.walrex.application.port.output.QrCodePort;
import org.walrex.infrastructure.adapter.outbound.qrcode_emv.EmvQrCodeAdapter;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EmvQrLogicTest {

    private EmvQrService emvQrService;
    private QrCodePort qrCodePort;
    private MerchantQrOutputPort merchantQrOutputPort;

    @BeforeEach
    void setUp() {
        qrCodePort = new EmvQrCodeAdapter(); // Use the real adapter as it has no dependencies
        merchantQrOutputPort = Mockito.mock(MerchantQrOutputPort.class);
        emvQrService = new EmvQrService();
        emvQrService.qrCodePort = qrCodePort;
        emvQrService.merchantQrOutputPort = merchantQrOutputPort;
    }

    @Test
    void shouldGenerateAndDecodeVenezuelaSuiche7BQr() {
        // Given
        GenerateQrRequest request = GenerateQrRequest.builder()
                .merchantId("010204261996491V17346883")
                .merchantName("Orelin Cabrera")
                .city("CARACAS")
                .qrType(GenerateQrRequest.QrType.STATIC)
                .amount(BigDecimal.ZERO)
                .currency("928")
                .countryCode("VE")
                .mcc("5411")
                .build();

        // When
        String generated = emvQrService.generateQr(request).await().indefinitely();

        // Then
        System.out.println("Generated VE QR: " + generated);
        assertThat(generated).startsWith("000201");
        assertThat(generated).contains("com.suiche7b.ve");
        assertThat(generated).contains("5303928");
        assertThat(generated).contains("010204261996491V17346883");

        // Decode check
        DecodeQrResponse decoded = emvQrService.decodeQr(generated).await().indefinitely();
        assertThat(decoded.getMerchantId()).isEqualTo("010204261996491V17346883");
        assertThat(decoded.getMerchantName()).isEqualTo("Orelin Cabrera");
        assertThat(decoded.getCurrency()).isEqualTo("928");
    }
}
