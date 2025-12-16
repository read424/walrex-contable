package org.walrex.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@Data
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Customer {

    private Integer id;

    private Integer idTypeDocument;

    private String numberDocument;

    private String firstName;

    private String lastName;

    private String address;

    private Integer idCountryDepartment;

    private Integer idCountryProvince;

    private Integer idCountryDistrict;

    private String gender;

    private String email;

    private LocalDate birthDate;

    private Integer idProfessional;

    @Builder.Default
    private String isPEP = "0";

    private Integer idCountryResidence;

    private String phoneNumber;

    private String phoneMobile;

    private Integer idCountryPhone;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private OffsetDateTime deletedAt;
}
