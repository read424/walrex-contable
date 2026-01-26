package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.ClientRepositoryPort;
import org.walrex.domain.exception.DuplicateCustomerException;
import org.walrex.domain.model.Customer;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CustomerEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.CustomerEntityMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.CustomerRepository;

@Slf4j
@ApplicationScoped
public class ClientRepositoryAdapter implements ClientRepositoryPort {

    @Inject
    CustomerRepository customerRepository;

    @Inject
    CustomerEntityMapper customerEntityMapper;

    @Override
    public Uni<Integer> save(Customer customer) {
        log.debug("Saving customer: email={}, phone={}", customer.getEmail(), customer.getPhoneNumber());

        // Verificar duplicados por email
        Uni<Boolean> emailCheck = customer.getEmail() != null
                ? customerRepository.existsByEmail(customer.getEmail(), null)
                : Uni.createFrom().item(false);

        return emailCheck
                .onItem().transformToUni(emailExists -> {
                    if (emailExists) {
                        log.warn("Duplicate email found: {}", customer.getEmail());
                        return Uni.createFrom().failure(
                                new DuplicateCustomerException("email", customer.getEmail())
                        );
                    }

                    // Mapear a entidad y persistir
                    CustomerEntity entity = customerEntityMapper.toEntity(customer);
                    log.debug("Persisting customer entity");

                    return customerRepository.persist(entity)
                            .onItem().transform(savedEntity -> {
                                log.info("Customer saved with ID: {}", savedEntity.getId());
                                return savedEntity.getId();
                            });
                });
    }
}
