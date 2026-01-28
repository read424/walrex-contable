package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.WalletTransactionEntity;

@ApplicationScoped
public class WalletTransactionRepository implements PanacheRepository<WalletTransactionEntity> {
}
