package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

/**
 * Entidad JPA para price_exchange
 * Almacena tasas de cambio obtenidas de proveedores externos
 */
@Entity
@Table(name = "price_exchange")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PriceExchangeEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "type_operation", length = 1, nullable = false)
    private String typeOperation;  // '3' para REMESAS

    @Column(name = "id_currency_base", nullable = false)//aqui ahora iran los id de country_currencies
    private Integer idCurrencyBase;  // Ej: 5 (USD/USDT)

    @Column(name = "id_currency_quote", nullable = false)//aqui ahora iran los id de country_currencies
    private Integer idCurrencyQuote;  // Ej: 4 (PEN) o 3 (VES)

    @Column(name = "amount_price", precision = 13, scale = 5, nullable = false)
    private BigDecimal amountPrice;  // Promedio de las 5 mejores tasas

    @Column(name = "is_active", length = 1, nullable = false)
    private String isActive;  // '1' = activo, '0' = inactivo

    @Column(name = "date_exchange")
    private LocalDate dateExchange;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (isActive == null) {
            isActive = "1";
        }
        if (dateExchange == null) {
            dateExchange = LocalDate.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetTime.now();
    }
}
