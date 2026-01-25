package org.walrex.domain.model;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED
}
