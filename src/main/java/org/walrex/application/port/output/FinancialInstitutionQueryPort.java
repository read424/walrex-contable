package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.InstitutionPayoutRailEntity;

import java.util.List;

public interface FinancialInstitutionQueryPort {
    Uni<List<InstitutionPayoutRailEntity>> findByRailCodeAndCountryIso2(String methodType, String countryIso2);
}
