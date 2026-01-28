package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.port.output.WalletCountryConfigRepositoryPort;
import org.walrex.domain.model.WalletCountryConfig;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.WalletCountryConfigEntityMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.WalletCountryConfigRepository;

import java.util.List;

@ApplicationScoped
public class WalletCountryConfigRepositoryAdapter implements WalletCountryConfigRepositoryPort {

    @Inject
    WalletCountryConfigRepository walletCountryConfigRepository;

    @Inject
    WalletCountryConfigEntityMapper walletCountryConfigEntityMapper;

    @Override
    public Uni<List<WalletCountryConfig>> findDefaultsByCountryId(Integer countryId) {
        return walletCountryConfigRepository
                .find("countryId = ?1 AND isDefault = true AND enabled = true", countryId)
                .list()
                .map(walletCountryConfigEntityMapper::toDomainList);
    }
}
