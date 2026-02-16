package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.walrex.application.dto.query.BeneficiaryAccountFilter;
import org.walrex.application.dto.query.PageRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class BeneficiaryAccountCacheKeyGenerator {

    private static final String CACHE_PREFIX = "beneficiary_account:list:";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String generateKey(PageRequest pageRequest, BeneficiaryAccountFilter filter) {
        try {
            CacheKeyComponents components = new CacheKeyComponents(
                    pageRequest.getPage(),
                    pageRequest.getSize(),
                    pageRequest.getSortBy(),
                    pageRequest.getSortDirection().getValue(),
                    filter != null ? filter.getCustomerId() : null,
                    filter != null ? filter.getAccountNumber() : null
            );

            String jsonRepresentation = objectMapper.writeValueAsString(components);
            String hash = generateSHA256Hash(jsonRepresentation);
            return CACHE_PREFIX + hash;
        } catch (JsonProcessingException e) {
            return CACHE_PREFIX + buildFallbackKey(pageRequest, filter);
        }
    }

    public static String getInvalidationPattern() {
        return CACHE_PREFIX + "*";
    }

    private static String generateSHA256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }

    private static String buildFallbackKey(PageRequest pageRequest, BeneficiaryAccountFilter filter) {
        StringBuilder key = new StringBuilder();
        key.append("page:").append(pageRequest.getPage())
                .append(":size:").append(pageRequest.getSize())
                .append(":sort:").append(pageRequest.getSortBy())
                .append(":dir:").append(pageRequest.getSortDirection().getValue());

        if (filter != null) {
            if (filter.getCustomerId() != null) {
                key.append(":customerId:").append(filter.getCustomerId());
            }
            if (filter.getAccountNumber() != null) {
                key.append(":accountNumber:").append(filter.getAccountNumber());
            }
        }
        return String.valueOf(key.toString().hashCode());
    }

    private record CacheKeyComponents(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            Long customerId,
            String accountNumber
    ) {}
}
