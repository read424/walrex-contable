package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "clients", uniqueConstraints = {
        @UniqueConstraint(name = "email_client_unique", columnNames = { "det_email" }),
        @UniqueConstraint(name = "id_clien_pk", columnNames = { "id" })
})
public class CustomerEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_type_document")
    private Integer idTypeDocument;

    @Column(name = "num_dni")
    private String numberDocument;

    @Column(name = "apellidos")
    private String lastName;

    @Column(name = "nombres")
    private String firstName;

    @Column(name = "sexo")
    private String gender;

    @Column(name = "det_email")
    private String email;

    @Column(name = "date_birth")
    private LocalDate birthDate;

    @Column(name = "id_profesion")
    private Integer idProfessional;

    @Column(name = "is_pep")
    private String isPEP;

    @Column(name = "id_country_resident")
    private Integer idCountryResidence;

    @Column(name = "id_departamento")
    private Integer idCountryDepartment;

    @Column(name = "id_provincia")
    private Integer idCountryProvince;

    @Column(name = "id_distrito")
    private Integer idCountryDistrict;

    @Column(name = "phonemobile")
    private String phoneMobile;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "id_country_phone")
    private Integer idCountryPhone;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Builder.Default
    @Column(name = "kyc_status")
    public String kycStatus = "PENDING";

    @Builder.Default
    @Column(name = "kyc_level", nullable = false)
    public Integer kycLevel = 0;

    // Screening
    @Builder.Default
    @Column(name = "screening_decision")
    private String screeningDecision = "PENDING";

    @Builder.Default
    @Column(name = "screening_score")
    private BigDecimal screeningScore = BigDecimal.ZERO;

    @Column(name = "screening_datasets")
    private String screeningDatasets;

    @Column(name = "screening_entity_id")
    private String screeningEntityId;

    @Column(name = "screening_last_checked")
    private OffsetDateTime screeningLastChecked;

    // KYC extendido
    @Column(name = "kyc_reviewed_by")
    private Integer kycReviewedBy;

    @Column(name = "kyc_reviewed_at")
    private OffsetDateTime kycReviewedAt;

    @Column(name = "kyc_approved_at")
    private OffsetDateTime kycApprovedAt;

    @Column(name = "kyc_expires_at")
    private LocalDate kycExpiresAt;

    @Column(name = "kyc_notes")
    private String kycNotes;

    // Nacionalidad / país de nacimiento
    @Column(name = "nationality")
    private String nationality;

    @Column(name = "id_country_birth")
    private Integer idCountryBirth;
}
