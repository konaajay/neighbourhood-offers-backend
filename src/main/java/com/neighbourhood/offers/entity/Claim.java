package com.neighbourhood.offers.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims", indexes = {
    @Index(name = "idx_claim_code", columnList = "claim_code", unique = true),
    @Index(name = "idx_idempotency_key", columnList = "idempotency_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_code", nullable = false, unique = true)
    private String claimCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shopper_id", nullable = false)
    private User shopper;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ClaimStatus status = ClaimStatus.CLAIMED;

    @Column(nullable = false)
    private LocalDateTime claimedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime redeemedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redeemed_by_staff_id")
    private User redeemedByStaff;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    private BigDecimal billAmountEntered;

    @Column(columnDefinition = "TEXT")
    private String cartSnapshotJson;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedTotal;

    @PrePersist
    protected void onCreate() {
        if (claimedAt == null) {
            claimedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ClaimStatus.CLAIMED;
        }
        if (expiresAt == null && offer != null && offer.getEndDate() != null) {
            expiresAt = offer.getEndDate();
        } else if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusDays(3);
        }
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
}
