package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.port.output.ProductVariantRepositoryPort;
import org.walrex.domain.model.ProductVariant;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.ProductVariantMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.ProductVariantRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Adaptador de persistencia que implementa el puerto de salida para ProductVariant.
 *
 * Siguiendo el patrón hexagonal (Ports & Adapters), este adaptador:
 * - Implementa ProductVariantRepositoryPort
 * - Traduce entre el modelo de dominio (ProductVariant) y la capa de persistencia
 *   (ProductVariantEntity)
 * - Utiliza el mapper para transformaciones
 * - Delega operaciones de persistencia al repository de Panache
 *
 * NOTA: Este adaptador es MINIMAL e implementa solo la operación save() requerida
 * por ProductTemplateService para crear variantes automáticamente.
 */
@ApplicationScoped
public class ProductVariantPersistenceAdapter implements ProductVariantRepositoryPort {

    @Inject
    ProductVariantRepository repository;

    @Inject
    ProductVariantMapper mapper;

    /**
     * Persiste una nueva variante de producto.
     *
     * Operación:
     * 1. Mapea el dominio a entidad
     * 2. Establece createdAt = ahora (si id es null)
     * 3. Establece updatedAt = ahora siempre
     * 4. Persiste la entidad
     * 5. Mapea la entidad persistida de vuelta a dominio
     *
     * @param productVariant Entidad de dominio a persistir
     * @return Uni con la variante persistida incluyendo el ID generado
     */
    @Override
    public Uni<ProductVariant> save(ProductVariant productVariant) {
        var entity = mapper.toEntity(productVariant);

        // Establecer timestamps
        if (entity.getId() == null) {
            entity.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        // Persistir y retornar el dominio
        return repository.persist(entity)
                .onItem().transform(mapper::toDomain);
    }
}
