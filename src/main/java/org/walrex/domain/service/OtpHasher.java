package org.walrex.domain.service;

public interface OtpHasher {
    String hash(String raw);
    boolean matches(String raw, String hash);
}
