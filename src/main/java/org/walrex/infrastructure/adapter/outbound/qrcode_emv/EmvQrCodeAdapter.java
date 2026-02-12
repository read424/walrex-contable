package org.walrex.infrastructure.adapter.outbound.qrcode_emv;

import com.emv.qrcode.model.mpm.MerchantAccountInformationTemplate;
import com.emv.qrcode.model.mpm.MerchantPresentedMode;
import com.emv.qrcode.model.mpm.MerchantAccountInformationReserved;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.QrCodePort;
import org.walrex.domain.model.EmvQrCode;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ApplicationScoped
public class EmvQrCodeAdapter implements QrCodePort {

    @Override
    public Uni<String> encode(EmvQrCode data) {
        return Uni.createFrom().item(() -> {
            try {
                MerchantPresentedMode mpm = new MerchantPresentedMode();
                mpm.setPayloadFormatIndicator(data.getPayloadFormatIndicator());
                mpm.setPointOfInitiationMethod(data.getPointOfInitiationMethod());
                
                if (data.getMerchantAccountInfo() != null) {
                    data.getMerchantAccountInfo().forEach((tag, value) -> {
                        MerchantAccountInformationReserved infoValue = new MerchantAccountInformationReserved(value);
                        MerchantAccountInformationTemplate template = new MerchantAccountInformationTemplate(tag, infoValue);
                        mpm.addMerchantAccountInformation(template);
                    });
                }
                
                mpm.setMerchantCategoryCode(data.getMerchantCategoryCode());
                mpm.setTransactionCurrency(data.getTransactionCurrency());
                if (data.getTransactionAmount() != null) {
                    mpm.setTransactionAmount(data.getTransactionAmount().toPlainString());
                }
                mpm.setCountryCode(data.getCountryCode());
                mpm.setMerchantName(data.getMerchantName());
                mpm.setMerchantCity(data.getMerchantCity());
                mpm.setPostalCode(data.getPostalCode());
                
                return mpm.toString() != null ? mpm.toString() : "";
            } catch (Exception e) {
                log.error("Error encoding EMV QR Code", e);
                throw new RuntimeException("Failed to encode EMV QR Code", e);
            }
        });
    }

    @Override
    public Uni<EmvQrCode> decode(String qrCode) {
        return Uni.createFrom().item(() -> {
            try {
                log.debug("Manual decode of QR Code: {}", qrCode);
                Map<String, String> tags = parseTlv(qrCode);
                
                Map<String, String> accountInfoMap = new HashMap<>();
                // Tags 02-51 are Merchant Account Information (02-25 are sometimes used similarly)
                for (int i = 2; i <= 51; i++) {
                    String tagStr = String.format("%02d", i);
                    if (tags.containsKey(tagStr)) {
                        accountInfoMap.put(tagStr, tags.get(tagStr));
                    }
                }

                return EmvQrCode.builder()
                        .payloadFormatIndicator(tags.get("00"))
                        .pointOfInitiationMethod(tags.get("01"))
                        .merchantAccountInfo(accountInfoMap)
                        .merchantCategoryCode(tags.get("52"))
                        .transactionCurrency(tags.get("53"))
                        .transactionAmount(tags.containsKey("54") ? new BigDecimal(tags.get("54")) : null)
                        .countryCode(tags.get("58"))
                        .merchantName(tags.get("59"))
                        .merchantCity(tags.get("60"))
                        .postalCode(tags.get("61"))
                        .additionalDataFieldTemplate(tags.get("62"))
                        .rawTags(tags)
                        .build();
            } catch (Exception e) {
                log.error("Error decoding EMV QR Code: {}", qrCode, e);
                // Si falla el parseo manual (ej: NumberFormatException en BigDecimal), lanzamos excepción controlada
                throw new RuntimeException("Failed to decode EMV QR Code: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Parseador manual robusto de TLV (Tag-Length-Value) para EMV QR.
     * Evita la recursividad automática que causan algunas librerías en tags de información de cuenta.
     */
    private Map<String, String> parseTlv(String qrCode) {
        Map<String, String> tags = new HashMap<>();
        int i = 0;
        int maxLen = qrCode.length();

        while (i < maxLen - 4) { // Necesitamos al menos 4 chars para Tag(2) y Length(2)
            try {
                String tag = qrCode.substring(i, i + 2);
                String lenStr = qrCode.substring(i + 2, i + 4);
                int len = Integer.parseInt(lenStr);
                
                i += 4;
                if (i + len > maxLen) {
                    log.warn("TLV parsing reached end of string unexpectedly at index {} for tag {}. Truncating value.", i, tag);
                    String value = qrCode.substring(i);
                    tags.put(tag, value);
                    break;
                }
                
                String value = qrCode.substring(i, i + len);
                tags.put(tag, value);
                i += len;
                
                // Si llegamos al CRC (Tag 63), usualmente es el último tag
                if ("63".equals(tag)) {
                    break;
                }
            } catch (NumberFormatException e) {
                log.error("Invalid length format in QR at index {}", i + 2, e);
                break;
            } catch (Exception e) {
                log.error("Error parsing TLV at index {}", i, e);
                break;
            }
        }
        return tags;
    }
}
