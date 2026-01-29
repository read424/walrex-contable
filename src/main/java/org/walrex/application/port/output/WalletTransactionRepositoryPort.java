package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.WalletTransaction;

import java.util.List;

public interface WalletTransactionRepositoryPort {

    Uni<List<WalletTransaction>> findRecentByWalletIds(List<Long> walletIds, int limit);
}
