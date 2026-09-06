package com.neighbourhood.offers.dto;

import com.neighbourhood.offers.entity.Claim;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedemptionReceiptDto {
    private String claimCode;
    private String status;
    private Long shopId;
    private String shopName;
    private String shopperName;
    private String offerTitle;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal billAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalBillAmount;
    private LocalDateTime redeemedAt;
    private String redeemedByStaffName;
    private Integer pointsDeducted;
    private Integer remainingPointsBalance;
    private Boolean isIdempotentReplay;
    private String message;

    public static RedemptionReceiptDto fromClaim(Claim claim, boolean isReplay, int pointsDeducted, int remainingBalance) {
        BigDecimal bill = claim.getBillAmountEntered();
        BigDecimal discountAmt = BigDecimal.ZERO;
        BigDecimal finalBill = bill;

        if (bill != null && claim.getOffer() != null) {
            String type = claim.getOffer().getDiscountType();
            BigDecimal val = claim.getOffer().getDiscountValue();

            if ("PERCENTAGE".equalsIgnoreCase(type) && val != null) {
                discountAmt = bill.multiply(val).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                if (claim.getOffer().getMaxDiscountAmount() != null && discountAmt.compareTo(claim.getOffer().getMaxDiscountAmount()) > 0) {
                    discountAmt = claim.getOffer().getMaxDiscountAmount();
                }
            } else if ("FLAT_AMOUNT".equalsIgnoreCase(type) && val != null) {
                discountAmt = bill.min(val);
            } else if ("BOGO".equalsIgnoreCase(type)) {
                discountAmt = val != null ? val : BigDecimal.ZERO;
            }
            finalBill = bill.subtract(discountAmt).max(BigDecimal.ZERO);
        }

        return RedemptionReceiptDto.builder()
                .claimCode(claim.getClaimCode())
                .status(claim.getStatus().name())
                .shopId(claim.getShop().getId())
                .shopName(claim.getShop().getName())
                .shopperName(claim.getShopper() != null ? claim.getShopper().getFullName() : "Customer")
                .offerTitle(claim.getOffer().getTitle())
                .discountType(claim.getOffer().getDiscountType())
                .discountValue(claim.getOffer().getDiscountValue())
                .billAmount(bill)
                .discountAmount(discountAmt)
                .finalBillAmount(finalBill)
                .redeemedAt(claim.getRedeemedAt())
                .redeemedByStaffName(claim.getRedeemedByStaff() != null ? claim.getRedeemedByStaff().getFullName() : "Counter Staff")
                .pointsDeducted(pointsDeducted)
                .remainingPointsBalance(remainingBalance)
                .isIdempotentReplay(isReplay)
                .message(isReplay 
                    ? "Notice: This claim was previously redeemed. Idempotent receipt replayed without additional point deduction."
                    : "Redemption successful! Discount applied and footfall recorded.")
                .build();
    }
}
