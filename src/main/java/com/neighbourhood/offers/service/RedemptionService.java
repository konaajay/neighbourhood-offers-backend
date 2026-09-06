package com.neighbourhood.offers.service;

import com.neighbourhood.offers.dto.LookupClaimResponseDto;
import com.neighbourhood.offers.dto.RedeemRequest;
import com.neighbourhood.offers.dto.RedemptionReceiptDto;
import com.neighbourhood.offers.entity.*;
import com.neighbourhood.offers.exception.ClaimAlreadyRedeemedException;
import com.neighbourhood.offers.exception.InsufficientPointsException;
import com.neighbourhood.offers.exception.OfferExpiredException;
import com.neighbourhood.offers.exception.ResourceNotFoundException;
import com.neighbourhood.offers.exception.UnauthorizedShopAccessException;
import com.neighbourhood.offers.repository.ClaimRepository;
import com.neighbourhood.offers.repository.PointTransactionRepository;
import com.neighbourhood.offers.repository.ShopRepository;
import com.neighbourhood.offers.repository.UserRepository;
import com.neighbourhood.offers.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.neighbourhood.offers.exception.InvalidBillAmountException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedemptionService {

    private final ClaimRepository claimRepository;
    private final ShopRepository shopRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public LookupClaimResponseDto lookupClaim(String claimCode, UserPrincipal staff) {
        String cleanCode = claimCode != null ? claimCode.trim().toUpperCase() : "";

        Claim claim = claimRepository.findByClaimCode(cleanCode)
                .orElseThrow(() -> new ResourceNotFoundException("No claim found with code: " + cleanCode));

        // Validate counter staff shop assignment
        if (staff.getShopId() == null || !claim.getShop().getId().equals(staff.getShopId())) {
            throw new UnauthorizedShopAccessException(
                    String.format("Cross-shop mismatch: This voucher is for '%s' (Shop #%d), but your counter is for Shop #%d.",
                            claim.getShop().getName(), claim.getShop().getId(), staff.getShopId()));
        }

        boolean isEligible = true;
        String rejectionReason = null;

        if (claim.getStatus() == ClaimStatus.REDEEMED) {
            throw new ClaimAlreadyRedeemedException(
                    "Voucher already redeemed: This voucher has already been used and cannot be redeemed again.");
        } else if (claim.isExpired()) {
            throw new OfferExpiredException(
                    "Voucher expired: This voucher expired on " + claim.getExpiresAt() + " and cannot be redeemed.");
        } else if (claim.getShop().getPointsBalance() < claim.getShop().getCostPerRedemption()) {
            isEligible = false;
            rejectionReason = "Insufficient points to redeem this offer.";
        }

        return LookupClaimResponseDto.builder()
                .claimCode(claim.getClaimCode())
                .status(claim.getStatus())
                .offerId(claim.getOffer().getId())
                .offerTitle(claim.getOffer().getTitle())
                .discountType(claim.getOffer().getDiscountType())
                .discountValue(claim.getOffer().getDiscountValue())
                .minBillAmount(claim.getOffer().getMinBillAmount())
                .shopId(claim.getShop().getId())
                .shopName(claim.getShop().getName())
                .shopperId(claim.getShopper() != null ? claim.getShopper().getId() : null)
                .shopperName(claim.getShopper() != null ? claim.getShopper().getFullName() : "Customer")
                .claimedAt(claim.getClaimedAt())
                .expiresAt(claim.getExpiresAt())
                .isEligible(isEligible)
                .rejectionReason(rejectionReason)
                .cartSnapshotJson(claim.getCartSnapshotJson())
                .estimatedTotal(claim.getEstimatedTotal())
                .costPerRedemption(claim.getShop().getCostPerRedemption())
                .build();
    }

    /**
     * Idempotent, transaction-safe counter redemption.
     * Uses pessimistic write locks on both the Claim row and Shop row to prevent race conditions
     * under unstable cellular connections where staff taps 'Redeem' multiple times rapidly.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public RedemptionReceiptDto redeemClaim(RedeemRequest request, UserPrincipal staff) {
        String cleanCode = request.getClaimCode() != null ? request.getClaimCode().trim().toUpperCase() : "";

        // 1. Acquire pessimistic lock on the Claim record
        Claim claim = claimRepository.findByClaimCodeForUpdate(cleanCode)
                .orElseThrow(() -> new ResourceNotFoundException("Claim voucher not found: " + cleanCode));

        // 2. Validate tenant isolation: Staff must belong to the claim's shop
        if (staff.getShopId() == null || !claim.getShop().getId().equals(staff.getShopId())) {
            throw new UnauthorizedShopAccessException(
                    String.format("Cross-shop mismatch: This claim belongs to shop '%s' (ID %d), not your shop (ID %d).",
                            claim.getShop().getName(), claim.getShop().getId(), staff.getShopId()));
        }

        // 3. IDEMPOTENCY SAFEGUARD:
        // If already redeemed (e.g. rapid multi-taps by counter staff over lagging network),
        // gracefully return the existing receipt without double-deducting points!
        if (claim.getStatus() == ClaimStatus.REDEEMED) {
            if (request.getIdempotencyKey() != null && request.getIdempotencyKey().equals(claim.getIdempotencyKey())) {
                log.info("Idempotent replay detected for claimCode={} by staff={}", cleanCode, staff.getUsername());
                Shop shop = shopRepository.findById(claim.getShop().getId()).orElse(claim.getShop());
                return RedemptionReceiptDto.fromClaim(claim, true, 0, shop.getPointsBalance());
            }
            throw new ClaimAlreadyRedeemedException("Voucher already redeemed: This voucher has already been used and cannot be redeemed again.");
        }

        // 4. Validate voucher expiration
        if (claim.isExpired()) {
            throw new OfferExpiredException("This claim voucher expired on " + claim.getExpiresAt());
        }

        // 5. Backend Minimum Bill Validation (Do not rely only on frontend!)
        BigDecimal minBill = claim.getOffer().getMinBillAmount();
        if (minBill != null && minBill.compareTo(BigDecimal.ZERO) > 0) {
            if (request.getBillAmount() == null || request.getBillAmount().compareTo(minBill) < 0) {
                throw new InvalidBillAmountException(
                        String.format("Bill amount (₹%s) does not meet the minimum required bill of ₹%s for this offer. Redemption blocked.",
                                request.getBillAmount() != null ? request.getBillAmount() : BigDecimal.ZERO,
                                minBill));
            }
        }

        // 6. Pessimistic lock on Shop to ensure point balance integrity
        Shop shop = shopRepository.findByIdForUpdate(claim.getShop().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Associated shop not found"));

        int cost = shop.getCostPerRedemption();
        if (shop.getPointsBalance() < cost) {
            throw new InsufficientPointsException("Insufficient points to redeem this offer.");
        }

        // 6. Deduct points atomically
        int newBalance = shop.getPointsBalance() - cost;
        shop.setPointsBalance(newBalance);
        shopRepository.save(shop);

        // 7. Update Claim status
        User staffUser = userRepository.findById(staff.getId()).orElse(null);
        claim.setStatus(ClaimStatus.REDEEMED);
        claim.setRedeemedAt(LocalDateTime.now());
        claim.setRedeemedByStaff(staffUser);
        claim.setIdempotencyKey(request.getIdempotencyKey());
        claim.setBillAmountEntered(request.getBillAmount());
        claimRepository.save(claim);

        // 8. Update Offer stats
        Offer offer = claim.getOffer();
        offer.setRedemptionCount(offer.getRedemptionCount() + 1);

        // 9. Append immutable transaction to ledger
        PointTransaction pt = PointTransaction.builder()
                .shop(shop)
                .claim(claim)
                .transactionType(TransactionType.REDEMPTION_DEBIT)
                .pointsAmount(-cost)
                .balanceAfter(newBalance)
                .description("Redemption of claim " + cleanCode + " for offer '" + offer.getTitle() + "'")
                .createdAt(LocalDateTime.now())
                .build();
        pointTransactionRepository.save(pt);

        log.info("Successfully redeemed claimCode={} for shopId={}. Deducted {} pts, new balance: {}",
                cleanCode, shop.getId(), cost, newBalance);

        return RedemptionReceiptDto.fromClaim(claim, false, cost, newBalance);
    }
}
