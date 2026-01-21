package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryAccount {
    public Integer id;
    public Customer customer;
    public Bank bank;
    public TypeAccountBank typeAccount;
    public String accountNumber;
    public String beneficiaryLastName;
    public String beneficiarySurname;
    public String idNumber;
    public String status;
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;
    public TypeOperation typeOperation;
    public String isAccountMe;
}
