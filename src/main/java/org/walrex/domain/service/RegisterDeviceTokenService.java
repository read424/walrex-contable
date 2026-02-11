package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.input.RegisterDeviceTokenUseCase;
import org.walrex.application.port.output.DeviceTokenRepositoryPort;
import org.walrex.domain.model.DeviceToken;
import org.walrex.domain.model.PlatformType;

@Slf4j
@ApplicationScoped
public class RegisterDeviceTokenService implements RegisterDeviceTokenUseCase {

    @Inject
    DeviceTokenRepositoryPort deviceTokenRepositoryPort;

    @Override
    public Uni<DeviceToken> register(Integer userId, String token, String platform) {
        log.info("Registering device token for userId: {}, platform: {}", userId, platform);

        PlatformType platformType = PlatformType.valueOf(platform.toUpperCase());

        return deviceTokenRepositoryPort.findByToken(token)
                .onItem().transformToUni(existing -> {
                    if (existing != null) {
                        log.info("Token already exists, updating userId and reactivating: {}", token);
                        existing.setUserId(userId);
                        existing.setPlatform(platformType);
                        existing.setActive(true);
                        return deviceTokenRepositoryPort.save(existing);
                    }

                    log.info("Creating new device token for userId: {}", userId);
                    DeviceToken newToken = DeviceToken.builder()
                            .userId(userId)
                            .token(token)
                            .platform(platformType)
                            .active(true)
                            .build();
                    return deviceTokenRepositoryPort.save(newToken);
                });
    }
}
