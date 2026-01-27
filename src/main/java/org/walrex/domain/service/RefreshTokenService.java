package org.walrex.domain.service;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.walrex.application.dto.request.RefreshTokenRequest;
import org.walrex.application.dto.response.LoginResponse;
import org.walrex.application.port.input.RefreshTokenUseCase;
import org.walrex.application.port.output.RefreshTokenRepositoryPort;
import org.walrex.application.port.output.UserRepositoryPort;
import org.walrex.domain.exception.InvalidTokenException;
import org.walrex.domain.model.RefreshToken;
import org.walrex.domain.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Optional;

@ApplicationScoped
public class RefreshTokenService implements RefreshTokenUseCase {

    @Inject
    RefreshTokenRepositoryPort refreshTokenRepository;

    @Inject
    UserRepositoryPort userRepository;

    @Inject
    TokenService tokenService;

    @Inject
    JsonWebToken jwt;

    @Override
    @WithTransaction
    public Uni<LoginResponse> refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String token = refreshTokenRequest.refreshToken();
        String tokenHash = hashToken(token);

        return refreshTokenRepository.findByTokenHash(tokenHash)
                .onItem().transformToUni(tokenOpt -> {
                    if (tokenOpt.isEmpty() || tokenOpt.get().getRevokedAt() != null || tokenOpt.get().getExpiresAt().isBefore(OffsetDateTime.now())) {
                        return Uni.createFrom().failure(new InvalidTokenException("Invalid refresh token"));
                    }
                    RefreshToken oldToken = tokenOpt.get();
                    oldToken.setRevokedAt(OffsetDateTime.now());

                    return refreshTokenRepository.update(oldToken)
                            .onItem().transformToUni(revoked -> fetchUserAndGenerateTokens(oldToken.getUserId()));
                });
    }

    private Uni<LoginResponse> fetchUserAndGenerateTokens(Integer userId) {
        return userRepository.findById(userId)
                .onItem().ifNull().failWith(() -> new InvalidTokenException("User not found for token"))
                .onItem().transformToUni(userOpt -> {
                    User user = userOpt.orElseThrow(() -> new InvalidTokenException("User not found"));
                    return tokenService.generateTokens(user)
                            .onItem().transformToUni(loginResponse -> {
                                RefreshToken newRefreshToken = RefreshToken.builder()
                                        .userId(user.getId())
                                        .refreshTokenHash(hashToken(loginResponse.refreshToken()))
                                        .expiresAt(OffsetDateTime.now().plusDays(15))
                                        .build();

                                return refreshTokenRepository.save(newRefreshToken)
                                        .onItem().transform(saved -> loginResponse);
                            });
                });
    }


    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not hash token", e);
        }
    }
}
