package com.neighbourhood.offers.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String discountType; // PERCENTAGE, FLAT_AMOUNT, BOGO

    @Column(nullable = false)
    private BigDecimal discountValue;

    private BigDecimal minBillAmount;

    private BigDecimal maxDiscountAmount;

    private String applicableCategory;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Builder.Default
    private Integer claimLimit = 50;

    @Builder.Default
    private Integer claimsCount = 0;

    @Builder.Default
    private Integer redemptionCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OfferStatus status = OfferStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String rawAiPrompt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (claimsCount == null) {
            claimsCount = 0;
        }
        if (redemptionCount == null) {
            redemptionCount = 0;
        }
        if (status == null) {
            status = OfferStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return endDate != null && endDate.isBefore(LocalDateTime.now());
    }

    public boolean isClaimable() {
        return status == OfferStatus.ACTIVE && !isExpired();
    }
}
