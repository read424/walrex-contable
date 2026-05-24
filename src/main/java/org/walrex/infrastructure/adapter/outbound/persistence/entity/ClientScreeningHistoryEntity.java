package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "client_screening_history")
public class ClientScreeningHistoryEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "client_id", nullable = false)
    private Integer clientId;

    @Column(name = "decision", nullable = false)
    private String decision;

    @Column(name = "score", nullable = false)
    private BigDecimal score;

    @Column(name = "datasets")
    private String datasets;

    @Column(name = "entity_id")
    private String entityId;

    @Builder.Default
    @Column(name = "identifier_matched")
    private Boolean identifierMatched = false;

    @Builder.Default
    @Column(name = "triggered_by")
    private String triggeredBy = "AUTO";

    @Column(name = "checked_at")
    private OffsetDateTime checkedAt;
}
