package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;

import java.util.Map;

public interface PushNotificationPort {

    Uni<Void> sendToDevice(String deviceToken, Map<String, String> data);

    Uni<Void> sendToAllActiveDevices(Map<String, String> data);
}
