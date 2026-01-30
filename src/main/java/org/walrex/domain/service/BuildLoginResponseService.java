package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.dto.response.LoginResponse;
import org.walrex.application.dto.response.WalletDetailDTO;
import org.walrex.application.port.input.BuildLoginResponseUseCase;
import org.walrex.application.port.output.AccountWalletRepositoryPort;
import org.walrex.application.port.output.ClientRepositoryPort;
import org.walrex.application.port.output.WalletTransactionRepositoryPort;
import org.walrex.domain.model.Customer;
import org.walrex.domain.model.User;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class BuildLoginResponseService implements BuildLoginResponseUseCase {

    @Inject
    ClientRepositoryPort clientRepositoryPort;

    @Inject
    AccountWalletRepositoryPort accountWalletRepositoryPort;

    @Inject
    WalletTransactionRepositoryPort walletTransactionRepositoryPort;

    @Inject
    TokenService tokenService;

    @ConfigProperty(name = "app.login.recent-transactions-limit", defaultValue = "10")
    int recentTransactionsLimit;

    @Override
    public Uni<LoginResponse> buildResponse(User user) {
        log.debug("Building login response for userId: {}, customerId: {}", user.getId(), user.getCustomerId());

        // Fetch customer data
        Uni<Customer> customerUni = clientRepositoryPort.findById(user.getCustomerId())
                .map(opt -> opt.orElse(null));

        // Fetch wallets with details
        Uni<List<WalletDetailDTO>> walletsUni = accountWalletRepositoryPort
                .findWalletsWithDetailsByClientId(user.getCustomerId());

        // Combine customer and wallets, then fetch transactions
        return Uni.combine().all().unis(customerUni, walletsUni)
                .asTuple()
                .flatMap(tuple -> {
                    Customer customer = tuple.getItem1();
                    List<WalletDetailDTO> wallets = tuple.getItem2();

                    // Get wallet IDs for transaction query
                    List<Long> walletIds = wallets.stream()
                            .map(WalletDetailDTO::getWalletId)
                            .toList();

                    // Create map of walletId -> currencyCode for transactions
                    Map<Long, String> walletCurrencyMap = wallets.stream()
                            .collect(Collectors.toMap(
                                    WalletDetailDTO::getWalletId,
                                    WalletDetailDTO::getCurrencyCode
                            ));

                    // Fetch recent transactions
                    return walletTransactionRepositoryPort
                            .findRecentByWalletIds(walletIds, recentTransactionsLimit)
                            .flatMap(transactions -> tokenService.generateTokens(
                                    user,
                                    customer,
                                    wallets,
                                    transactions,
                                    walletCurrencyMap
                            ));
                });
    }
}
