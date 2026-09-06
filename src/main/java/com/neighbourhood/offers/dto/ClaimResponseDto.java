package com.neighbourhood.offers.dto;

import com.neighbourhood.offers.entity.Claim;
import com.neighbourhood.offers.entity.ClaimStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponseDto {
    private Long id;
    private String claimCode;
    private Long offerId;
    private String offerTitle;
    private String offerDescription;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minBillAmount;
    private Long shopId;
    private String shopName;
    private String shopAddress;
    private String shopLocality;
    private Long shopperId;
    private String shopperName;
    private ClaimStatus status;
    private LocalDateTime claimedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime redeemedAt;
    private Boolean isExpired;
    private String cartSnapshotJson;
    private BigDecimal estimatedTotal;

    public static ClaimResponseDto fromEntity(Claim claim) {
        return ClaimResponseDto.builder()
                .id(claim.getId())
                .claimCode(claim.getClaimCode())
                .offerId(claim.getOffer().getId())
                .offerTitle(claim.getOffer().getTitle())
                .offerDescription(claim.getOffer().getDescription())
                .discountType(claim.getOffer().getDiscountType())
                .discountValue(claim.getOffer().getDiscountValue())
                .minBillAmount(claim.getOffer().getMinBillAmount())
                .shopId(claim.getShop().getId())
                .shopName(claim.getShop().getName())
                .shopAddress(claim.getShop().getAddress())
                .shopLocality(claim.getShop().getLocality())
                .shopperId(claim.getShopper().getId())
                .shopperName(claim.getShopper().getFullName())
                .status(claim.getStatus())
                .claimedAt(claim.getClaimedAt())
                .expiresAt(claim.getExpiresAt())
                .redeemedAt(claim.getRedeemedAt())
                .isExpired(claim.isExpired())
                .cartSnapshotJson(claim.getCartSnapshotJson())
                .estimatedTotal(claim.getEstimatedTotal())
                .build();
    }
}
