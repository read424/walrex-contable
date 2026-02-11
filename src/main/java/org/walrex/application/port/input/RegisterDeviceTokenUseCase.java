package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.DeviceToken;

public interface RegisterDeviceTokenUseCase {

    Uni<DeviceToken> register(Integer userId, String token, String platform);
}
