package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.WalletDetailDTO;
import org.walrex.domain.model.AccountWallet;

import java.util.List;

public interface AccountWalletRepositoryPort {

    Uni<AccountWallet> save(AccountWallet accountWallet);

    Uni<List<AccountWallet>> saveAll(List<AccountWallet> accountWallets);

    Uni<List<AccountWallet>> findByClientId(Integer clientId);

    Uni<List<WalletDetailDTO>> findWalletsWithDetailsByClientId(Integer clientId);
}
