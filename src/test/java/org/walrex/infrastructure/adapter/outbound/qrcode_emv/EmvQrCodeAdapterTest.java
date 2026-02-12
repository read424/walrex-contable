package org.walrex.infrastructure.adapter.outbound.qrcode_emv;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.walrex.domain.model.EmvQrCode;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmvQrCodeAdapterTest {

    EmvQrCodeAdapter adapter = new EmvQrCodeAdapter();

    @Test
    void shouldEncodeAndDecodeSuccessfully() {
        // Given
        EmvQrCode original = EmvQrCode.builder()
                .payloadFormatIndicator("01")
                .pointOfInitiationMethod("12")
                .merchantAccountInfo(Map.of("02", "1234567890"))
                .merchantCategoryCode("5411")
                .transactionCurrency("604") // PEN
                .transactionAmount(new BigDecimal("150.00"))
                .countryCode("PE")
                .merchantName("WALREX TEST")
                .merchantCity("LIMA")
                .build();

        // When
        String encoded = adapter.encode(original).await().indefinitely();
        EmvQrCode decoded = adapter.decode(encoded).await().indefinitely();

        // Then
        assertThat(decoded.getMerchantName()).isEqualTo(original.getMerchantName());
        assertThat(decoded.getTransactionAmount()).isEqualByComparingTo(original.getTransactionAmount());
        assertThat(decoded.getTransactionCurrency()).isEqualTo(original.getTransactionCurrency());
        assertThat(decoded.getCountryCode()).isEqualTo(original.getCountryCode());
        // Note: The template value might be prefixed or wrapped depending on the library's toString()
        assertThat(decoded.getMerchantAccountInfo().get("02")).contains("1234567890");
    }

    @Test
    void shouldDecodeKnownString() {
        // Example EMV QR String (Merchant Presented Mode)
        // IDs: 00 (Payload), 01 (Method), 52 (MCC), 53 (Currency), 54 (Amount), 58 (Country), 59 (Name), 60 (City), 63 (CRC)
        String qrString = "0002010102115204541153036045406150.005802PE5911WALREX TEST6004LIMA630460A2";
        
        // When
        EmvQrCode decoded = adapter.decode(qrString).await().indefinitely();

        // Then
        assertThat(decoded.getMerchantName()).isEqualTo("WALREX TEST");
        assertThat(decoded.getMerchantCity()).isEqualTo("LIMA");
        assertThat(decoded.getTransactionCurrency()).isEqualTo("604");
        assertThat(decoded.getTransactionAmount()).isEqualByComparingTo("150.00");
    }

    @Test
    void shouldGenerateQrWithAmount50PEN() {
        // Given
        EmvQrCode data = EmvQrCode.builder()
                .payloadFormatIndicator("01")
                .pointOfInitiationMethod("12")
                .merchantCategoryCode("5411")
                .transactionCurrency("604") // PEN
                .transactionAmount(new BigDecimal("50.00"))
                .countryCode("PE")
                .merchantName("WALREX STORE")
                .merchantCity("LIMA")
                .build();

        // When
        String encoded = adapter.encode(data).await().indefinitely();
        System.out.println("QR with 50 PEN: " + encoded);

        // Then
        assertThat(encoded).contains("540550.00");
        assertThat(encoded).contains("5303604");
    }

    @Test
    void shouldGenerateQrWithoutAmount() {
        // Given
        EmvQrCode data = EmvQrCode.builder()
                .payloadFormatIndicator("01")
                .pointOfInitiationMethod("11") // Static
                .merchantCategoryCode("5411")
                .transactionCurrency("604")
                .countryCode("PE")
                .merchantName("WALREX STORE")
                .merchantCity("LIMA")
                .build();

        // When
        String encoded = adapter.encode(data).await().indefinitely();
        System.out.println("QR without amount: " + encoded);

        // Then
        assertThat(encoded).doesNotContain("540"); // Tag 54 is Amount
    }

    @Test
    void shouldMatchYapeReferenceExactly() {
        // Reference string provided by user
        // 00020101021139329b5f61085cf35843a22e4211b0a97fd15204561153036045802PE5906YAPERO6004Lima630410D0
        
        EmvQrCode reference = EmvQrCode.builder()
                .payloadFormatIndicator("01")
                .pointOfInitiationMethod("11")
                .merchantAccountInfo(Map.of("39", "9b5f61085cf35843a22e4211b0a97fd1"))
                .merchantCategoryCode("5611")
                .transactionCurrency("604")
                .countryCode("PE")
                .merchantName("YAPERO")
                .merchantCity("Lima")
                .build();

        String generated = adapter.encode(reference).await().indefinitely();
        String expected = "00020101021139329b5f61085cf35843a22e4211b0a97fd15204561153036045802PE5906YAPERO6004Lima630410D0";
        
        System.out.println("Generated: " + generated);
        System.out.println("Expected:  " + expected);
        
        assertThat(generated).isEqualTo(expected);
    }
}
