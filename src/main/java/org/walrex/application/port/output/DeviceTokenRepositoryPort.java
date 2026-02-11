package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.DeviceToken;

import java.util.List;

public interface DeviceTokenRepositoryPort {

    Uni<DeviceToken> save(DeviceToken deviceToken);

    Uni<List<DeviceToken>> findActiveByUserId(Integer userId);

    Uni<DeviceToken> findByToken(String token);

    Uni<List<DeviceToken>> findAllActive();

    Uni<Void> deactivate(String token);
}
