package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Entidad para la tabla banks
 * Representa bancos/métodos de pago disponibles por país
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "financial_institution")
public class BankEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sigla", length = 8, nullable = false)
    private String sigla;

    @Column(name = "det_name", length = 80, nullable = false)
    private String detName;

    @Column(name = "id_country", nullable = false)
    private Integer idCountry;

    @Builder.Default
    @Column(name = "status", length = 1, nullable = false)
    private String status = "1";

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "codigo", length = 5)
    private String codigo;

    /**
     * Código del método de pago para Binance P2P
     * Ejemplo: "Yape", "Plin", "BancoDeCredito", "ScotiabankPeru"
     */
    @Column(name = "name_pay_binance", length = 50)
    private String namePayBinance;
}
