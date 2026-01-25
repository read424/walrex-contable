package org.walrex;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class RandomPortTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.http.test-port", "0");
    }
}
