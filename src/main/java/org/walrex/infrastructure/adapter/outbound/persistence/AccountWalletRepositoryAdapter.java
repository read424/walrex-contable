package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.AccountWalletRepositoryPort;
import org.walrex.domain.model.AccountWallet;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.AccountWalletEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.AccountWalletEntityMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.AccountWalletRepository;

import java.util.List;

@Slf4j
@ApplicationScoped
public class AccountWalletRepositoryAdapter implements AccountWalletRepositoryPort {

    @Inject
    AccountWalletRepository accountWalletRepository;

    @Inject
    AccountWalletEntityMapper accountWalletEntityMapper;

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
}
