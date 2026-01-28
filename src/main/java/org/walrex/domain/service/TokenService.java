package org.walrex.domain.service;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.dto.response.LoginResponse;
import org.walrex.application.dto.response.WalletDetailDTO;
import org.walrex.domain.model.Customer;
import org.walrex.domain.model.User;
import org.walrex.domain.model.WalletTransaction;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @ConfigProperty(name = "mp.jwt.secret")
    String secret;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public Uni<LoginResponse> generateTokens(User user) {
        return generateTokens(user, null, List.of(), List.of(), Map.of());
    }

    public Uni<LoginResponse> generateTokens(
            User user,
            Customer customer,
            List<WalletDetailDTO> wallets,
            List<WalletTransaction> recentTransactions,
            Map<Long, String> walletCurrencyMap) {

        long accessTokenExpiration = System.currentTimeMillis() / 1000 + 600; // 10 minutes
        long refreshTokenExpiration = System.currentTimeMillis() / 1000 + 1296000; // 15 days

        String accessToken = Jwt.issuer(issuer)
                .subject(user.getId().toString())
                .upn(user.getUsername())
                .groups(new HashSet<>(Arrays.asList("user")))
                .expiresAt(accessTokenExpiration)
                .signWithSecret(secret);

        String refreshToken = Jwt.issuer(issuer)
                .subject(user.getId().toString())
                .upn("refresh_token")
                .expiresAt(refreshTokenExpiration)
                .signWithSecret(secret);

        // Build user info
        LoginResponse.UserInfo userInfo = null;
        if (customer != null) {
            String fullName = buildFullName(customer);
            userInfo = LoginResponse.UserInfo.builder()
                    .id(user.getId())
                    .name(fullName)
                    .kyc(LoginResponse.KycInfo.builder()
                            .status(customer.getKycStatus())
                            .level(customer.getKycLevel())
                            .build())
                    .build();
        }

        // Build wallets info
        List<LoginResponse.WalletInfo> walletInfos = wallets.stream()
                .map(w -> LoginResponse.WalletInfo.builder()
                        .walletId(w.getWalletId())
                        .country(w.getCountryCode())
                        .currency(w.getCurrencyCode())
                        .balance(w.getBalance())
                        .viewBalance(w.getViewBalance())
                        .isDefault(w.getIsDefault())
                        .build())
                .toList();

        // Build transactions info
        List<LoginResponse.TransactionInfo> transactionInfos = recentTransactions.stream()
                .map(t -> LoginResponse.TransactionInfo.builder()
                        .id(t.getId().toString())
                        .type(t.getOperationType())
                        .amount(t.getAmount())
                        .currency(walletCurrencyMap.getOrDefault(t.getWalletId(), ""))
                        .counterparty(t.getCounterpartyReference())
                        .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().format(DATE_FORMATTER) : null)
                        .build())
                .toList();

        return Uni.createFrom().item(LoginResponse.builder()
                .accessToken(accessToken)
                .expiresIn(600L)
                .refreshToken(refreshToken)
                .refreshExpiresIn(1296000L)
                .tokenType("Bearer")
                .user(userInfo)
                .wallets(walletInfos)
                .recentTransactions(transactionInfos)
                .build());
    }

    private String buildFullName(Customer customer) {
        StringBuilder name = new StringBuilder();
        if (customer.getFirstName() != null) {
            name.append(customer.getFirstName());
        }
        if (customer.getLastName() != null) {
            if (name.length() > 0) name.append(" ");
            name.append(customer.getLastName());
        }
        return name.toString();
    }
}
