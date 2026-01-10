package org.walrex.infrastructure.adapter.outbound.persistence;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.PaymentMethodQueryPort;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.CountryCurrencyPaymentMethodRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Persistence adapter for querying payment methods
 */
@Slf4j
@ApplicationScoped
public class PaymentMethodPersistenceAdapter implements PaymentMethodQueryPort {

    @Inject
    CountryCurrencyPaymentMethodRepository repository;

    @Override
    @WithSession
    public Uni<List<String>> findBinancePaymentCodesByCountryCurrency(Long countryCurrencyId) {
        log.debug("Querying payment methods for country_currency: {}", countryCurrencyId);

        return repository.findActiveByCountryCurrencyId(countryCurrencyId)
                .onItem().invoke(paymentCodes ->
                    log.info("Found {} payment methods for country_currency {}: {}",
                            paymentCodes.size(), countryCurrencyId, paymentCodes)
                )
                .onFailure().invoke(error ->
                        log.error("Error querying payment methods for country_currency {}: {}",
                                countryCurrencyId, error.getMessage())
                )
                .onFailure().recoverWithItem(Collections.emptyList());
    }
}
