package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.walrex.domain.model.AccountType;
import org.walrex.domain.model.NormalSide;
import org.walrex.infrastructure.adapter.outbound.listener.AccountEntityListener;
import org.walrex.infrastructure.adapter.outbound.persistence.converter.AccountTypeConverter;
import org.walrex.infrastructure.adapter.outbound.persistence.converter.NormalSideConverter;

import java.time.OffsetDateTime;

/**
 * Entidad JPA para cuentas contables.
 *
 * Mapea la tabla 'accountingAccounts' en PostgreSQL.
 * Usa Panache para simplificar operaciones de persistencia reactiva.
 *
 * Configuración:
 * - Extends PanacheEntityBase: Proporciona métodos helper de Panache
 * - @Entity: Marca esta clase como entidad JPA
 * - @Table: Especifica el nombre de la tabla y constraints únicos
 * - Lombok: Genera getters, setters, constructores, etc.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "accounts", uniqueConstraints = {
        @UniqueConstraint(name = "accounts_code_key", columnNames = { "code" }),
        @UniqueConstraint(name = "accounts_name_key", columnNames = { "name", "code" })
})
@EntityListeners(AccountEntityListener.class)
public class AccountingAccountEntity extends PanacheEntityBase {

    /**
     * Identificador único generado automáticamente
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Código único de la cuenta (máximo 20 caracteres)
     */
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    /**
     * Nombre descriptivo de la cuenta (máximo 200 caracteres)
     */
    @Column(name = "name", nullable = false, unique = true, length = 200)
    private String name;

    /**
     * Tipo contable de la cuenta
     * Se almacena como string en la DB usando el enum PostgreSQL account_type.
     * La conversión es manejada por {@link AccountTypeConverter}.
     */
    @Column(name = "type", nullable = false)
    private AccountType type;

    /**
     * Lado normal de la cuenta (DEBIT o CREDIT)
     * Se almacena como string en la DB usando el enum PostgreSQL normal_side.
     * La conversión es manejada por {@link NormalSideConverter}.
     */
    @Column(name = "normal_side", nullable = false, columnDefinition = "TEXT")
    private NormalSide normalSide;

    /**
     * Indica si la cuenta está activa
     */
    @Column(name = "is_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean active;

    /**
     * Indica si la cuenta ya fue sincronizada con la base de datos vectorial (Qdrant)
     */
    @Column(name = "embeddings_synced", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean embeddingsSynced;

    /**
     * Fecha de creación del registro
     */
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    /**
     * Fecha de última actualización del registro
     */
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    /**
     * Fecha de eliminación lógica del registro
     * Si es null, el registro no ha sido eliminado
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
