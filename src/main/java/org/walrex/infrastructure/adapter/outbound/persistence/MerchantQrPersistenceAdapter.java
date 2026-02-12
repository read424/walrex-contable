package org.walrex.infrastructure.adapter.outbound.persistence;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.port.output.MerchantQrOutputPort;
import org.walrex.domain.model.MerchantQr;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.MerchantQrEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.MerchantQrRepository;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class MerchantQrPersistenceAdapter implements MerchantQrOutputPort {

    @Inject
    MerchantQrRepository repository;

    @Override
    public Uni<MerchantQr> save(MerchantQr merchantQr) {
        MerchantQrEntity entity = toEntity(merchantQr);
        return Panache.withTransaction(() -> repository.persist(entity))
                .replaceWith(() -> toDomain(entity));
    }

    @Override
    public Uni<MerchantQr> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Uni<List<MerchantQr>> findAll() {
        return repository.listAll()
                .map(entities -> entities.stream()
                        .map(this::toDomain)
                        .collect(Collectors.toList()));
    }

    @Override
    public Uni<Boolean> delete(Long id) {
        return Panache.withTransaction(() -> repository.deleteById(id));
    }

    private MerchantQr toDomain(MerchantQrEntity entity) {
        if (entity == null) return null;
        return MerchantQr.builder()
                .id(entity.getId())
                .name(entity.getName())
                .merchantName(entity.getMerchantName())
                .merchantCity(entity.getMerchantCity())
                .mcc(entity.getMcc())
                .currency(entity.getCurrency())
                .countryCode(entity.getCountryCode())
                .payloadFormatIndicator(entity.getPayloadFormatIndicator())
                .pointOfInitiationMethod(entity.getPointOfInitiationMethod())
                .accountInfo(entity.getAccountInfo())
                .build();
    }

    private MerchantQrEntity toEntity(MerchantQr merchantQr) {
        if (merchantQr == null) return null;
        return MerchantQrEntity.builder()
                .id(merchantQr.getId())
                .name(merchantQr.getName())
                .merchantName(merchantQr.getMerchantName())
                .merchantCity(merchantQr.getMerchantCity())
                .mcc(merchantQr.getMcc())
                .currency(merchantQr.getCurrency())
                .countryCode(merchantQr.getCountryCode())
                .payloadFormatIndicator(merchantQr.getPayloadFormatIndicator())
                .pointOfInitiationMethod(merchantQr.getPointOfInitiationMethod())
                .accountInfo(merchantQr.getAccountInfo())
                .build();
    }
}
