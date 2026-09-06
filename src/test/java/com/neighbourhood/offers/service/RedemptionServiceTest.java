package com.neighbourhood.offers.service;

import com.neighbourhood.offers.dto.RedeemRequest;
import com.neighbourhood.offers.dto.RedemptionReceiptDto;
import com.neighbourhood.offers.entity.*;
import com.neighbourhood.offers.exception.InsufficientPointsException;
import com.neighbourhood.offers.exception.InvalidBillAmountException;
import com.neighbourhood.offers.exception.OfferExpiredException;
import com.neighbourhood.offers.exception.UnauthorizedShopAccessException;
import com.neighbourhood.offers.repository.ClaimRepository;
import com.neighbourhood.offers.repository.OfferRepository;
import com.neighbourhood.offers.repository.ShopRepository;
import com.neighbourhood.offers.repository.UserRepository;
import com.neighbourhood.offers.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class RedemptionServiceTest {

    @Autowired
    private RedemptionService redemptionService;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private UserRepository userRepository;

    private Shop shop;
    private Offer offer;
    private User staff;
    private User shopper;
    private UserPrincipal staffPrincipal;

    @BeforeEach
    void setUp() {
        shop = shopRepository.save(Shop.builder()
                .name("Kavitha Sarees")
                .category("Apparel")
                .pointsBalance(100)
                .costPerRedemption(10)
                .build());

        staff = userRepository.save(User.builder()
                .email("kavitha_staff_" + System.currentTimeMillis() + "@test.com")
                .password("secret")
                .fullName("Kavitha Staff")
                .role(Role.ROLE_COUNTER_STAFF)
                .shop(shop)
                .build());

        shopper = userRepository.save(User.builder()
                .email("shopper_" + System.currentTimeMillis() + "@test.com")
                .password("secret")
                .fullName("Anita Shopper")
                .role(Role.ROLE_SHOPPER)
                .build());

        offer = offerRepository.save(Offer.builder()
                .shop(shop)
                .title("20% Off Sarees")
                .discountType("PERCENTAGE")
                .discountValue(BigDecimal.valueOf(20))
                .status(OfferStatus.ACTIVE)
                .endDate(LocalDateTime.now().plusDays(10))
                .build());

        staffPrincipal = UserPrincipal.create(staff);
    }

    @Test
    @DisplayName("Redeeming a valid claim deducts 10 points and marks claim as REDEEMED")
    @Transactional
    void testSuccessfulRedemption() {
        Claim claim = claimRepository.save(Claim.builder()
                .claimCode("NBR-TEST-01")
                .offer(offer)
                .shop(shop)
                .shopper(shopper)
                .status(ClaimStatus.CLAIMED)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build());

        RedeemRequest request = RedeemRequest.builder()
                .claimCode("NBR-TEST-01")
                .idempotencyKey("idem-key-1")
                .billAmount(BigDecimal.valueOf(1500.0))
                .build();

        RedemptionReceiptDto receipt = redemptionService.redeemClaim(request, staffPrincipal);

        assertNotNull(receipt);
        assertEquals("REDEEMED", receipt.getStatus());
        assertEquals(10, receipt.getPointsDeducted());
        assertEquals(90, receipt.getRemainingPointsBalance());
        assertFalse(receipt.getIsIdempotentReplay());

        // Verify in database
        Shop updatedShop = shopRepository.findById(shop.getId()).orElseThrow();
        assertEquals(90, updatedShop.getPointsBalance());

        Claim updatedClaim = claimRepository.findByClaimCode("NBR-TEST-01").orElseThrow();
        assertEquals(ClaimStatus.REDEEMED, updatedClaim.getStatus());
    }

    @Test
    @DisplayName("Idempotency safeguard: Double-tapping redeem returns receipt without deducting points twice")
    @Transactional
    void testIdempotentDoubleRedeem() {
        Claim claim = claimRepository.save(Claim.builder()
                .claimCode("NBR-IDEM-02")
                .offer(offer)
                .shop(shop)
                .shopper(shopper)
                .status(ClaimStatus.CLAIMED)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build());

        RedeemRequest request1 = RedeemRequest.builder()
                .claimCode("NBR-IDEM-02")
                .idempotencyKey("idem-tap-1")
                .billAmount(BigDecimal.valueOf(1500.0))
                .build();

        // First tap: processes redemption
        RedemptionReceiptDto receipt1 = redemptionService.redeemClaim(request1, staffPrincipal);
        assertEquals("REDEEMED", receipt1.getStatus());
        assertEquals(90, receipt1.getRemainingPointsBalance());
        assertFalse(receipt1.getIsIdempotentReplay());

        // Second tap (e.g. lagging network retry): MUST return idempotent replay and deduct 0 points
        RedeemRequest request2 = RedeemRequest.builder()
                .claimCode("NBR-IDEM-02")
                .idempotencyKey("idem-tap-2")
                .billAmount(BigDecimal.valueOf(1500.0))
                .build();

        RedemptionReceiptDto receipt2 = redemptionService.redeemClaim(request2, staffPrincipal);
        assertEquals("REDEEMED", receipt2.getStatus());
        assertTrue(receipt2.getIsIdempotentReplay());
        assertEquals(0, receipt2.getPointsDeducted());

        // Crucial check: Shop points must STILL be 90, NOT 80!
        Shop updatedShop = shopRepository.findById(shop.getId()).orElseThrow();
        assertEquals(90, updatedShop.getPointsBalance());
    }

    @Test
    @DisplayName("Low points safeguard: Redeeming when points balance is depleted throws InsufficientPointsException")
    @Transactional
    void testInsufficientPointsThrowsException() {
        // Set shop points to 5 (< 10)
        shop.setPointsBalance(5);
        shopRepository.save(shop);

        Claim claim = claimRepository.save(Claim.builder()
                .claimCode("NBR-LOW-03")
                .offer(offer)
                .shop(shop)
                .shopper(shopper)
                .status(ClaimStatus.CLAIMED)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build());

        RedeemRequest request = RedeemRequest.builder()
                .claimCode("NBR-LOW-03")
                .billAmount(BigDecimal.valueOf(500.0))
                .build();

        assertThrows(InsufficientPointsException.class, () -> {
            redemptionService.redeemClaim(request, staffPrincipal);
        });

        // Verify claim is still unredeemed
        Claim untouchedClaim = claimRepository.findByClaimCode("NBR-LOW-03").orElseThrow();
        assertEquals(ClaimStatus.CLAIMED, untouchedClaim.getStatus());
        assertEquals(5, shopRepository.findById(shop.getId()).orElseThrow().getPointsBalance());
    }

    @Test
    @DisplayName("Expired claim vouchers cannot be redeemed")
    @Transactional
    void testExpiredClaimCannotBeRedeemed() {
        Claim expiredClaim = claimRepository.save(Claim.builder()
                .claimCode("NBR-EXP-04")
                .offer(offer)
                .shop(shop)
                .shopper(shopper)
                .status(ClaimStatus.CLAIMED)
                .expiresAt(LocalDateTime.now().minusDays(1)) // Expired yesterday
                .build());

        RedeemRequest request = RedeemRequest.builder()
                .claimCode("NBR-EXP-04")
                .build();

        assertThrows(OfferExpiredException.class, () -> {
            redemptionService.redeemClaim(request, staffPrincipal);
        });
    }

    @Test
    @DisplayName("Minimum bill safeguard: Bill below minBillAmount is rejected with 400 and 0 points deducted")
    @Transactional
    void testMinimumBillValidationFails() {
        offer.setMinBillAmount(BigDecimal.valueOf(1500));
        offerRepository.save(offer);

        Claim claim = claimRepository.save(Claim.builder()
                .claimCode("NBR-MIN-05")
                .offer(offer)
                .shop(shop)
                .shopper(shopper)
                .status(ClaimStatus.CLAIMED)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build());

        RedeemRequest request = RedeemRequest.builder()
                .claimCode("NBR-MIN-05")
                .billAmount(BigDecimal.valueOf(1200)) // Below 1500!
                .build();

        assertThrows(InvalidBillAmountException.class, () -> {
            redemptionService.redeemClaim(request, staffPrincipal);
        });

        // Points must remain 100, claim must remain CLAIMED
        Shop refreshedShop = shopRepository.findById(shop.getId()).orElseThrow();
        assertEquals(100, refreshedShop.getPointsBalance());

        Claim refreshedClaim = claimRepository.findByClaimCode("NBR-MIN-05").orElseThrow();
        assertEquals(ClaimStatus.CLAIMED, refreshedClaim.getStatus());
    }

    @Test
    @DisplayName("Minimum bill satisfied: Calculates discount and finalBillAmount accurately")
    @Transactional
    void testMinimumBillValidationPassesAndCalculatesDiscount() {
        offer.setMinBillAmount(BigDecimal.valueOf(1500));
        offer.setDiscountType("PERCENTAGE");
        offer.setDiscountValue(BigDecimal.valueOf(20)); // 20%
        offerRepository.save(offer);

        Claim claim = claimRepository.save(Claim.builder()
                .claimCode("NBR-MIN-06")
                .offer(offer)
                .shop(shop)
                .shopper(shopper)
                .status(ClaimStatus.CLAIMED)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build());

        RedeemRequest request = RedeemRequest.builder()
                .claimCode("NBR-MIN-06")
                .billAmount(BigDecimal.valueOf(2000))
                .build();

        RedemptionReceiptDto receipt = redemptionService.redeemClaim(request, staffPrincipal);

        assertNotNull(receipt);
        assertEquals("REDEEMED", receipt.getStatus());
        assertEquals(10, receipt.getPointsDeducted());
        assertEquals(90, receipt.getRemainingPointsBalance());
        assertEquals(0, BigDecimal.valueOf(400.00).compareTo(receipt.getDiscountAmount())); // 20% of 2000 = 400
        assertEquals(0, BigDecimal.valueOf(1600.00).compareTo(receipt.getFinalBillAmount())); // 2000 - 400 = 1600
    }
}
