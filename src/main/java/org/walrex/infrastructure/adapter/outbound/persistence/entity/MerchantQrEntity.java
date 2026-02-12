package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.walrex.infrastructure.adapter.outbound.persistence.converter.JsonMapConverter;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "merchant_qr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MerchantQrEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "merchant_name", length = 100)
    private String merchantName;

    @Column(name = "merchant_city", length = 100)
    private String merchantCity;

    @Column(name = "mcc", length = 4)
    private String mcc;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "payload_format_indicator", length = 2)
    private String payloadFormatIndicator;

    @Column(name = "point_of_initiation_method", length = 2)
    private String pointOfInitiationMethod;

    @Column(name = "account_info", columnDefinition = "TEXT")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, String> accountInfo;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
