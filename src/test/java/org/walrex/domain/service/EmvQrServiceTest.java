package org.walrex.domain.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.walrex.application.dto.response.DecodeQrResponse;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class EmvQrServiceTest {

    @Inject
    EmvQrService emvQrService;

    @Test
    void shouldDecodeYapeQrCorrectly() {
        // Given
        String yapeQr = "00020101021139329b5f61085cf35843a22e4211b0a97fd15204561153036045802PE5906YAPERO6004Lima630410D0";

        // When
        DecodeQrResponse response = emvQrService.decodeQr(yapeQr).await().indefinitely();

        // Then
        assertThat(response.getMerchantId()).isEqualTo("9b5f61085cf35843a22e4211b0a97fd1");
        assertThat(response.getMerchantName()).isEqualTo("YAPERO");
        assertThat(response.isValid()).isTrue();
    }

    @Test
    void shouldDecodePlinBbvaQrCorrectly() {
        // Given
        String plinBbva = "0002015802PE0102115204482953036045912P2P Transfer6004Lima26560116Plin Network P2P00327fc1134f89a44eaca4968de69885cf7d63047CC1";

        // When
        DecodeQrResponse response = emvQrService.decodeQr(plinBbva).await().indefinitely();

        // Then
        assertThat(response.getMerchantId()).isEqualTo("7fc1134f89a44eaca4968de69885cf7d");
        assertThat(response.getMerchantName()).isEqualTo("P2P Transfer");
        assertThat(response.isValid()).isTrue();
    }

    @Test
    void shouldDecodePlinScotiabankQrCorrectly() {
        // Given
        String plinScotiabank = "00020126560116Plin Network P2P0032e94bf92c5708496cb5c9fed3eb28203d0102115204482953036045912P2P Transfer6004Lima5802PE63041B7B";

        // When
        DecodeQrResponse response = emvQrService.decodeQr(plinScotiabank).await().indefinitely();

        // Then
        assertThat(response.getMerchantId()).isEqualTo("e94bf92c5708496cb5c9fed3eb28203d");
        assertThat(response.getMerchantName()).isEqualTo("P2P Transfer");
        assertThat(response.isValid()).isTrue();
    }

    @Test
    void shouldGenerateVenezuelaSuiche7BQrCorrectly() {
        // Given
        org.walrex.application.dto.request.GenerateQrRequest request = 
            org.walrex.application.dto.request.GenerateQrRequest.builder()
                .merchantId("010204261996491V17346883")
                .merchantName("Orelin Cabrera")
                .city("CARACAS")
                .qrType(org.walrex.application.dto.request.GenerateQrRequest.QrType.STATIC)
                .amount(java.math.BigDecimal.ZERO)
                .details("PAGO PRUEBA")
                .currency("928")
                .countryCode("VE")
                .mcc("5411")
                .build();

        // When
        String generated = emvQrService.generateQr(request).await().indefinitely();

        // Then
        // Verifica el texto: Debe empezar por 000201 y contener 26XXcom.suiche7b.ve y 5303928.
        assertThat(generated).startsWith("000201");
        assertThat(generated).contains("com.suiche7b.ve");
        assertThat(generated).contains("5303928");
        assertThat(generated).contains("010204261996491V17346883");
        
        System.out.println("Generated VE QR: " + generated);
        
        // Extra verification: Decode the generated QR and check merchantId
        DecodeQrResponse decoded = emvQrService.decodeQr(generated).await().indefinitely();
        assertThat(decoded.getMerchantId()).isEqualTo("010204261996491V17346883");
        assertThat(decoded.getMerchantName()).isEqualTo("Orelin Cabrera");
    }
}
