package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.WalletTransactionRepositoryPort;
import org.walrex.domain.model.WalletTransaction;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.WalletTransactionEntityMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.WalletTransactionRepository;

import java.util.List;

@Slf4j
@ApplicationScoped
public class WalletTransactionRepositoryAdapter implements WalletTransactionRepositoryPort {

    @Inject
    WalletTransactionRepository walletTransactionRepository;

    @Inject
    WalletTransactionEntityMapper walletTransactionEntityMapper;

    @Override
    public Uni<List<WalletTransaction>> findRecentByWalletIds(List<Long> walletIds, int limit) {
        if (walletIds == null || walletIds.isEmpty()) {
            return Uni.createFrom().item(List.of());
        }

        log.debug("Finding recent transactions for walletIds: {}, limit: {}", walletIds, limit);

        return walletTransactionRepository
                .find("walletId IN ?1 ORDER BY createdAt DESC", walletIds)
                .page(0, limit)
                .list()
                .map(walletTransactionEntityMapper::toDomainList);
    }
}
