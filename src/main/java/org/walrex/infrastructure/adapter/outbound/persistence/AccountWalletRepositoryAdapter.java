package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.reactive.mutiny.Mutiny;
import org.walrex.application.dto.response.WalletDetailDTO;
import org.walrex.application.port.output.AccountWalletRepositoryPort;
import org.walrex.domain.model.AccountWallet;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.AccountWalletEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.AccountWalletEntityMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.AccountWalletRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@ApplicationScoped
public class AccountWalletRepositoryAdapter implements AccountWalletRepositoryPort {

    @Inject
    AccountWalletRepository accountWalletRepository;

    @Inject
    AccountWalletEntityMapper accountWalletEntityMapper;

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Override
    public Uni<AccountWallet> save(AccountWallet accountWallet) {
        AccountWalletEntity entity = accountWalletEntityMapper.toEntity(accountWallet);
        return accountWalletRepository.persist(entity)
                .map(accountWalletEntityMapper::toDomain);
    }

    @Override
    public Uni<List<AccountWallet>> saveAll(List<AccountWallet> accountWallets) {
        List<AccountWalletEntity> entities = accountWalletEntityMapper.toEntityList(accountWallets);
        return accountWalletRepository.persist(entities)
                .replaceWith(accountWalletEntityMapper.toDomainList(entities));
    }

    @Override
    public Uni<List<AccountWallet>> findByClientId(Integer clientId) {
        return accountWalletRepository.find("clientId", clientId)
                .list()
                .map(accountWalletEntityMapper::toDomainList);
    }

    @Override
    public Uni<List<WalletDetailDTO>> findWalletsWithDetailsByClientId(Integer clientId) {
        log.debug("Finding wallets with details for clientId: {}", clientId);

        String query = """
            SELECT w.id as wallet_id,
                   c.code_iso3 as country_code,
                   cu.code_iso3 as currency_code,
                   w.available_balance as balance,
                   w.is_balance_visible as view_balance,
                   CASE WHEN ROW_NUMBER() OVER (ORDER BY w.id) = 1 THEN true ELSE false END as is_default
            FROM account_wallet w
            JOIN country c ON w.country_id = c.id
            JOIN currencies cu ON w.currency_id = cu.id
            WHERE w.client_id = :clientId AND w.status = 'ACTIVE'
            ORDER BY w.id
            """;

        return sessionFactory.withSession(session ->
                session.createNativeQuery(query, Object[].class)
                        .setParameter("clientId", clientId)
                        .getResultList()
                        .map(results -> {
                            List<WalletDetailDTO> wallets = new ArrayList<>();
                            for (Object[] row : results) {
                                wallets.add(WalletDetailDTO.builder()
                                        .walletId(((Number) row[0]).longValue())
                                        .countryCode((String) row[1])
                                        .currencyCode((String) row[2])
                                        .balance((BigDecimal) row[3])
                                        .viewBalance((Boolean) row[4])
                                        .isDefault((Boolean) row[5])
                                        .build());
                            }
                            return wallets;
                        })
        );
    }
}
