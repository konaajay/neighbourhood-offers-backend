package com.neighbourhood.offers.service;

import com.neighbourhood.offers.dto.ClaimResponseDto;
import com.neighbourhood.offers.dto.CreateClaimRequest;
import com.neighbourhood.offers.entity.*;
import com.neighbourhood.offers.exception.OfferExpiredException;
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
public class ClaimBusinessLogicTest {

    @Autowired
    private ClaimService claimService;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private UserRepository userRepository;

    private Shop shop;
    private Offer offer;
    private User shopper;
    private UserPrincipal shopperPrincipal;

    @BeforeEach
    void setUp() {
        shop = shopRepository.save(Shop.builder()
                .name("Bakery Corner")
                .pointsBalance(100)
                .build());

        shopper = userRepository.save(User.builder()
                .email("shopper_claim_" + System.currentTimeMillis() + "@test.com")
                .password("pass")
                .fullName("Shopper One")
                .role(Role.ROLE_SHOPPER)
                .build());

        offer = offerRepository.save(Offer.builder()
                .shop(shop)
                .title("Free Cupcake with Coffee")
                .discountType("FLAT_AMOUNT")
                .discountValue(BigDecimal.valueOf(50))
                .status(OfferStatus.ACTIVE)
                .endDate(LocalDateTime.now().plusDays(5))
                .build());

        shopperPrincipal = UserPrincipal.create(shopper);
    }

    @Test
    @DisplayName("Footfall promise: Claiming an offer does NOT deduct any points from the shopkeeper's balance")
    @Transactional
    void testClaimingDoesNotDeductPoints() {
        int initialBalance = shop.getPointsBalance();

        ClaimResponseDto claim = claimService.claimOffer(offer.getId(), shopperPrincipal);

        assertNotNull(claim);
        assertEquals(ClaimStatus.CLAIMED, claim.getStatus());
        assertTrue(claim.getClaimCode().startsWith("NBR-"));

        // Critical assertion: Shop balance must remain completely untouched!
        Shop fetchedShop = shopRepository.findById(shop.getId()).orElseThrow();
        assertEquals(initialBalance, fetchedShop.getPointsBalance(), "Shop points must NOT be deducted upon claim!");
    }

    @Test
    @DisplayName("Expired offers cannot be claimed")
    @Transactional
    void testExpiredOfferCannotBeClaimed() {
        Offer expiredOffer = offerRepository.save(Offer.builder()
                .shop(shop)
                .title("Expired Diwali Discount")
                .discountType("PERCENTAGE")
                .discountValue(BigDecimal.valueOf(10))
                .status(OfferStatus.ACTIVE)
                .endDate(LocalDateTime.now().minusHours(1)) // Expired
                .build());

        assertThrows(OfferExpiredException.class, () -> {
            claimService.claimOffer(expiredOffer.getId(), shopperPrincipal);
        });
    }

    @Test
    @DisplayName("Shopper cart claim stores cartSnapshotJson and estimatedTotal with 0 points deducted")
    @Transactional
    void testClaimWithCartSnapshotStoresSnapshotAndZeroPointDeduction() {
        int initialBalance = shop.getPointsBalance();
        String snapshotJson = "{\"items\":[{\"name\":\"Saree A\",\"price\":1200,\"quantity\":1}],\"subtotal\":1200}";
        CreateClaimRequest request = CreateClaimRequest.builder()
                .cartSnapshotJson(snapshotJson)
                .estimatedTotal(BigDecimal.valueOf(1200))
                .build();

        ClaimResponseDto claim = claimService.claimOffer(offer.getId(), request, shopperPrincipal);

        assertNotNull(claim);
        assertEquals(ClaimStatus.CLAIMED, claim.getStatus());
        assertEquals(snapshotJson, claim.getCartSnapshotJson());
        assertEquals(BigDecimal.valueOf(1200), claim.getEstimatedTotal());

        // Zero points deducted
        Shop fetchedShop = shopRepository.findById(shop.getId()).orElseThrow();
        assertEquals(initialBalance, fetchedShop.getPointsBalance(), "Shop points must remain untouched on cart claim!");
    }
}
