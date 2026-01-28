package org.walrex.domain.service;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.request.RegisterUserRequest;
import org.walrex.application.port.input.RegisterUserUseCase;
import org.walrex.application.port.output.AccountWalletRepositoryPort;
import org.walrex.application.port.output.ClientRepositoryPort;
import org.walrex.application.port.output.RegistrationTokenValidatorPort;
import org.walrex.application.port.output.UserRepositoryPort;
import org.walrex.application.port.output.WalletCountryConfigRepositoryPort;
import org.walrex.domain.model.AccountWallet;
import org.walrex.domain.model.Customer;
import org.walrex.domain.model.IdentificationMethod;
import org.walrex.domain.model.RegisteredUser;
import org.walrex.domain.model.User;
import org.walrex.infrastructure.adapter.inbound.mapper.RegisterUserMapper;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@ApplicationScoped
public class RegisterUserService implements RegisterUserUseCase {

    @Inject
    ClientRepositoryPort clientRepositoryPort;

    @Inject
    UserRepositoryPort userRepositoryPort;

    @Inject
    RegistrationTokenValidatorPort tokenValidatorPort;

    @Inject
    RegisterUserMapper registerUserMapper;

    @Inject
    WalletCountryConfigRepositoryPort walletCountryConfigRepositoryPort;

    @Inject
    AccountWalletRepositoryPort accountWalletRepositoryPort;

    @Override
    @WithTransaction
    public Uni<RegisteredUser> register(RegisterUserRequest request) {
        log.debug("Starting user registration process for referenceId: {}", request.getReferenceId());

        // PASO 1: Validar que el token OTP sea válido y extraer el target
        return tokenValidatorPort.validate(request.getRegistrationToken())
                .onItem().transformToUni(target -> {
                    log.debug("Registration token validated successfully, target: {}", target);

                    // PASO 2: Mapear request a Customer y setear email/phone según el método de identificación
                    Customer customer = registerUserMapper.toCustomer(request);
                    switch (request.getIdentificationMethod()) {
                        case EMAIL -> customer.setEmail(target);
                        case PHONE -> customer.setPhoneNumber(target);
                    }
                    log.debug("Mapped to Customer: firstName={}, lastName={}",
                            customer.getFirstName(),
                            customer.getLastName());

                    // PASO 3: Guardar Customer en BD
                    return clientRepositoryPort.save(customer)
                            .onItem().transformToUni(customerId -> {
                                log.info("Customer saved with ID: {}", customerId);

                                // PASO 4: Crear wallets por defecto para el cliente
                                Integer countryId = request.getCountryId();
                                return createDefaultWallets(customerId, countryId)
                                        .onItem().transformToUni(wallets -> {
                                            log.info("Created {} default wallets for customerId: {}", wallets.size(), customerId);

                                            // PASO 5: Mapear request a User
                                            User user = registerUserMapper.toUser(request, customerId);
                                            user.setUsername(target);
                                            log.debug("Mapped to User: username={}, type={}",
                                                    user.getUsername(),
                                                    user.getUsernameType());

                                            // PASO 6: Guardar User en BD
                                            return userRepositoryPort.save(user)
                                                    .onItem().transform(userId -> {
                                                        log.info("User saved with ID: {}, customerId: {}",
                                                                userId,
                                                                customerId);

                                                        // PASO 7: Retornar datos del usuario registrado
                                                        return RegisteredUser.builder()
                                                                .userId(userId.intValue())
                                                                .clientId(customerId)
                                                                .username(user.getUsername())
                                                                .createdAt(OffsetDateTime.now())
                                                                .build();
                                                    });
                                        });
                            });
                })
                .onFailure().invoke(failure -> {
                    log.error("Error during user registration: {}", failure.getMessage(), failure);
                });
    }

    private Uni<List<AccountWallet>> createDefaultWallets(Integer clientId, Integer countryId) {
        if (countryId == null) {
            log.warn("Country ID is null for clientId: {}, skipping wallet creation", clientId);
            return Uni.createFrom().item(List.of());
        }

        return walletCountryConfigRepositoryPort.findDefaultsByCountryId(countryId)
                .onItem().transformToUni(configs -> {
                    if (configs.isEmpty()) {
                        log.info("No default wallet configurations found for countryId: {}", countryId);
                        return Uni.createFrom().item(List.<AccountWallet>of());
                    }

                    List<AccountWallet> wallets = configs.stream()
                            .map(config -> AccountWallet.builder()
                                    .clientId(clientId)
                                    .countryId(config.getCountryId())
                                    .currencyId(config.getCurrencyId())
                                    .build())
                            .toList();

                    log.debug("Creating {} wallets for clientId: {}", wallets.size(), clientId);
                    return accountWalletRepositoryPort.saveAll(wallets);
                });
    }
}
