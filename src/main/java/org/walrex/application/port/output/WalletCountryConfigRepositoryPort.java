package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.WalletCountryConfig;

import java.util.List;

public interface WalletCountryConfigRepositoryPort {

    Uni<List<WalletCountryConfig>> findDefaultsByCountryId(Integer countryId);
}
