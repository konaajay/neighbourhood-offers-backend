package com.neighbourhood.offers.dto;

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
public class LookupClaimResponseDto {
    private String claimCode;
    private ClaimStatus status;
    private Long offerId;
    private String offerTitle;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minBillAmount;
    private Long shopId;
    private String shopName;
    private Long shopperId;
    private String shopperName;
    private LocalDateTime claimedAt;
    private LocalDateTime expiresAt;
    private Boolean isEligible;
    private String rejectionReason;
    private String cartSnapshotJson;
    private BigDecimal estimatedTotal;
    private Integer costPerRedemption;
}
