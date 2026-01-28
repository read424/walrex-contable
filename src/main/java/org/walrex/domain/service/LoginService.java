package org.walrex.domain.service;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.dto.request.LoginRequest;
import org.walrex.application.dto.response.LoginResponse;
import org.walrex.application.dto.response.WalletDetailDTO;
import org.walrex.application.port.input.LoginUseCase;
import org.walrex.application.port.output.AccountWalletRepositoryPort;
import org.walrex.application.port.output.ClientRepositoryPort;
import org.walrex.application.port.output.UserRepositoryPort;
import org.walrex.application.port.output.WalletTransactionRepositoryPort;
import org.walrex.domain.exception.InvalidCustomerDataException;
import org.walrex.domain.model.Customer;
import org.walrex.domain.model.User;
import org.walrex.domain.model.WalletTransaction;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class LoginService implements LoginUseCase {

    @Inject
    UserRepositoryPort userRepositoryPort;

    @Inject
    ClientRepositoryPort clientRepositoryPort;

    @Inject
    AccountWalletRepositoryPort accountWalletRepositoryPort;

    @Inject
    WalletTransactionRepositoryPort walletTransactionRepositoryPort;

    @Inject
    TokenService tokenService;

    @ConfigProperty(name = "app.login.max-attempts", defaultValue = "5")
    int maxLoginAttempts;

    @ConfigProperty(name = "app.login.lockout-minutes", defaultValue = "15")
    int lockoutMinutes;

    @ConfigProperty(name = "app.login.recent-transactions-limit", defaultValue = "10")
    int recentTransactionsLimit;

    @Override
    @WithTransaction
    public Uni<LoginResponse> login(LoginRequest loginRequest) {
        return userRepositoryPort.findByUsername(loginRequest.username())
                .flatMap(userOpt -> {
                    if (userOpt == null || userOpt.isEmpty()) {
                        return Uni.createFrom().failure(new InvalidCustomerDataException("Usuario o pin incorrectos"));
                    }

                    User user = userOpt.get();

                    if (user.getPinLockedUntil() != null && user.getPinLockedUntil().isAfter(OffsetDateTime.now())) {
                        return Uni.createFrom().failure(new InvalidCustomerDataException("La cuenta está bloqueada. Intente más tarde."));
                    }

                    if (loginRequest.pinHash().equals(user.getPinHash())) {
                        user.setPinAttempts(0);
                        user.setPinLockedUntil(null);
                        return userRepositoryPort.update(user)
                                .flatMap(updatedUser -> fetchUserDataAndGenerateTokens(updatedUser));
                    } else {
                        user.setPinAttempts(user.getPinAttempts() + 1);
                        if (user.getPinAttempts() >= maxLoginAttempts) {
                            user.setPinLockedUntil(OffsetDateTime.now().plusMinutes(lockoutMinutes));
                        }
                        return userRepositoryPort.update(user)
                                .flatMap(u -> Uni.createFrom().failure(new InvalidCustomerDataException("Usuario o pin incorrectos")));
                    }
                });
    }

    private Uni<LoginResponse> fetchUserDataAndGenerateTokens(User user) {
        log.debug("Fetching user data for clientId: {}", user.getCustomerId());

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
