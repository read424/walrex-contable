package org.walrex.domain.service;

import lombok.extern.slf4j.Slf4j;
import com.emv.qrcode.validators.Crc16Validate;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.request.GenerateQrRequest;
import org.walrex.application.dto.response.DecodeQrResponse;
import org.walrex.application.port.input.EmvQrUseCase;
import org.walrex.application.port.output.QrCodePort;
import org.walrex.domain.model.EmvQrCode;
import org.walrex.domain.model.MerchantQr;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ApplicationScoped
public class EmvQrService implements EmvQrUseCase, org.walrex.application.port.input.SaveMerchantQrUseCase {

    @Inject
    QrCodePort qrCodePort;

    @Inject
    org.walrex.application.port.output.MerchantQrOutputPort merchantQrOutputPort;

    @Override
    public Uni<String> generateQr(GenerateQrRequest request) {
        BigDecimal amount = request.getAmount();
        String countryCode = request.getCountryCode() != null ? request.getCountryCode() : "PE";
        String currency = request.getCurrency() != null ? request.getCurrency() : (countryCode.equals("VE") ? "928" : "604");
        String mcc = request.getMcc() != null ? request.getMcc() : "5611";
        
        // Point of Initiation Method: 11 (Static), 12 (Dynamic)
        // Si hay un monto cargado, se considera dinámico.
        boolean isDynamic = (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) || 
                          GenerateQrRequest.QrType.DYNAMIC.equals(request.getQrType());
        String initiationMethod = isDynamic ? "12" : "11";

        Map<String, String> merchantAccountInfo = new HashMap<>();
        
        if ("VE".equals(countryCode)) {
            // Lógica Suiche7B para Venezuela
            Map<String, String> s7bData = new HashMap<>();
            s7bData.put("00", "com.suiche7b.ve"); // GUID obligatorio
            s7bData.put("01", request.getMerchantId()); // Banco + Telefono + RIF
            
            // Suiche7B usa el Tag 26
            merchantAccountInfo.put("26", encodeSubTags(s7bData));
        } else {
            // Lógica por defecto (Peru / Yape)
            merchantAccountInfo.put("39", request.getMerchantId());
        }

        EmvQrCode emvQrCode = EmvQrCode.builder()
                .payloadFormatIndicator(request.getPayloadFormatIndicator() != null ? request.getPayloadFormatIndicator() : "01")
                .pointOfInitiationMethod(initiationMethod)
                .merchantAccountInfo(merchantAccountInfo)
                .merchantCategoryCode(mcc)
                .transactionCurrency(currency)
                .transactionAmount(amount != null && amount.compareTo(BigDecimal.ZERO) > 0 ? amount : null)
                .countryCode(countryCode)
                .merchantName(request.getMerchantName())
                .merchantCity(request.getCity())
                // Only include details if not empty
                .additionalDataFieldTemplate(request.getDetails() != null && !request.getDetails().isEmpty() 
                        ? request.getDetails() : null)
                .build();

        return qrCodePort.encode(emvQrCode);
    }

    /**
     * Auxiliar para codificar sub-tags TLV dentro de un template
     */
    private String encodeSubTags(Map<String, String> subTags) {
        StringBuilder sb = new StringBuilder();
        subTags.forEach((tag, value) -> {
            if (value != null) {
                sb.append(tag);
                sb.append(String.format("%02d", value.length()));
                sb.append(value);
            }
        });
        return sb.toString();
    }

    @Override
    public Uni<DecodeQrResponse> decodeQr(String qrCodeText) {
        return qrCodePort.decode(qrCodeText)
                .map(emvQr -> {
                    boolean isValid = Crc16Validate.validate(qrCodeText).isValid();
                    
                    log.info("=== [QR DECODED] Tags: {} ===", emvQr.getRawTags());
                    
                    String merchantId = extractMerchantId(emvQr);
                    
                    return DecodeQrResponse.builder()
                            .merchantId(merchantId)
                            .merchantName(emvQr.getMerchantName())
                            .city(emvQr.getMerchantCity())
                            .amount(emvQr.getTransactionAmount())
                            .currency(emvQr.getTransactionCurrency())
                            .valid(isValid)
                            .rawTags(emvQr.getRawTags())
                            .build();
                });
    }

    /**
     * Heurística para extraer el Merchant ID de diferentes implementaciones (Yape, Plin, etc.)
     */
    private String extractMerchantId(EmvQrCode emvQr) {
        if (emvQr.getMerchantAccountInfo() == null) return null;
        Map<String, String> accountInfo = emvQr.getMerchantAccountInfo();

        // 1. Caso Yape: Tag 39 directo
        if (accountInfo.containsKey("39")) {
            return accountInfo.get("39");
        }

        // 2. Caso Plin y otros: Tags 26-51 (Merchant Account Information Templates)
        // Buscamos en el rango 26-51 cualquier tag que contenga sub-tags
        for (int i = 26; i <= 51; i++) {
            String tagStr = String.format("%02d", i);
            if (accountInfo.containsKey(tagStr)) {
                String templateValue = accountInfo.get(tagStr);
                Map<String, String> subTags = parseSubTags(templateValue);
                
                // Prioridad 1: Sub-tag 01 (Usado por Suiche7B para Banco+Telefono+RIF)
                if (subTags.containsKey("01")) {
                    return subTags.get("01");
                }
                
                // Prioridad 2: Sub-tag 02 (Ocasionalmente usado para IDs de comercio)
                if (subTags.containsKey("02")) {
                    return subTags.get("02");
                }

                // Prioridad 3: Sub-tag 00 (Usado por Plin, siempre que no sea un GUID conocido)
                if (subTags.containsKey("00")) {
                    String val00 = subTags.get("00");
                    // Omitimos si parece un GUID de sistema (contiene puntos o es corto)
                    if (!val00.contains(".") && val00.length() >= 10) {
                        return val00;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Parseador básico de sub-tags TLV dentro de un template
     */
    private Map<String, String> parseSubTags(String value) {
        Map<String, String> subTags = new HashMap<>();
        if (value == null || value.length() < 4) return subTags;

        int i = 0;
        while (i < value.length() - 4) {
            try {
                String tag = value.substring(i, i + 2);
                int len = Integer.parseInt(value.substring(i + 2, i + 4));
                if (i + 4 + len <= value.length()) {
                    subTags.put(tag, value.substring(i + 4, i + 4 + len));
                }
                i += 4 + len;
            } catch (Exception e) {
                break;
            }
        }
        return subTags;
    }

    @Override
    public Uni<MerchantQr> saveMerchantQr(org.walrex.application.dto.request.SaveMerchantQrRequest request) {
        Map<String, String> tags = request.getRawTags();
        
        // Extract basic data for indexing/search
        Map<String, String> accountInfo = new HashMap<>();
        for (int i = 2; i <= 51; i++) {
            String tagStr = String.format("%02d", i);
            if (tags.containsKey(tagStr)) {
                accountInfo.put(tagStr, tags.get(tagStr));
            }
        }

        MerchantQr profile = MerchantQr.builder()
                .name(request.getName())
                .merchantName(tags.get("59"))
                .merchantCity(tags.get("60"))
                .mcc(tags.get("52"))
                .currency(tags.get("53"))
                .countryCode(tags.get("58"))
                .payloadFormatIndicator(tags.get("00"))
                .pointOfInitiationMethod(tags.get("01"))
                .accountInfo(accountInfo)
                .build();

        log.info("=== [SAVING MERCHANT QR] Name: {} | Merchant: {} ===", profile.getName(), profile.getMerchantName());
        return merchantQrOutputPort.save(profile);
    }

    @Override
    public Uni<List<MerchantQr>> getAllMerchantQrs() {
        return merchantQrOutputPort.findAll();
    }

    @Override
    public Uni<String> generateFromProfile(Long id, BigDecimal amount) {
        return merchantQrOutputPort.findById(id)
                .flatMap(profile -> {
                    if (profile == null) {
                        return Uni.createFrom().failure(new RuntimeException("Merchant QR profile not found"));
                    }

                    boolean hasAmount = amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
                    
                    EmvQrCode emvQrCode = EmvQrCode.builder()
                            .payloadFormatIndicator(profile.getPayloadFormatIndicator() != null ? profile.getPayloadFormatIndicator() : "01")
                            .pointOfInitiationMethod(hasAmount ? "12" : (profile.getPointOfInitiationMethod() != null ? profile.getPointOfInitiationMethod() : "11"))
                            .merchantAccountInfo(profile.getAccountInfo())
                            .merchantCategoryCode(profile.getMcc())
                            .transactionCurrency(profile.getCurrency())
                            .transactionAmount(hasAmount ? amount : null)
                            .countryCode(profile.getCountryCode())
                            .merchantName(profile.getMerchantName())
                            .merchantCity(profile.getMerchantCity())
                            .build();

                    return qrCodePort.encode(emvQrCode);
                });
    }

    @Override
    public Uni<Boolean> deleteMerchantQr(Long id) {
        return merchantQrOutputPort.delete(id);
    }
}
