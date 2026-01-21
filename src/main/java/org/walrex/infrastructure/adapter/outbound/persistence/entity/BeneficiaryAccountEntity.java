package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "accounts_beneficiary")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryAccountEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_client", nullable = false)
    public CustomerEntity customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_bank")
    public BankEntity bank;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_type_account")
    public TypeAccountBankEntity typeAccount;

    @Column(name = "number_account", nullable = false, length = 25)
    public String accountNumber;

    @Column(name = "last_name_benef", length = 60)
    public String beneficiaryLastName;

    @Column(name = "surname_benef", length = 50)
    public String beneficiarySurname;

    @Column(name = "number_id", nullable = false, length = 15)
    public String idNumber;

    @Column(length = 1)
    public String status;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_type_operation")
    public TypeOperationEntity typeOperation;

    @Column(name = "is_account_me", nullable = false, length = 1)
    public String isAccountMe;
}
