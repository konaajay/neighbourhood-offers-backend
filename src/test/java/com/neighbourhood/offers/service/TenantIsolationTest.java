package com.neighbourhood.offers.service;

import com.neighbourhood.offers.dto.CreateOfferRequest;
import com.neighbourhood.offers.dto.RedeemRequest;
import com.neighbourhood.offers.entity.*;
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
public class TenantIsolationTest {

    @Autowired
    private OfferService offerService;

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

    private Shop anithaShop;
    private Shop rahulShop;
    private User anitha;
    private User rahul;
    private User rahulStaff;
    private Offer anithaOffer;

    @BeforeEach
    void setUp() {
        anithaShop = shopRepository.save(Shop.builder()
                .name("Anitha Silks")
                .pointsBalance(200)
                .build());

        rahulShop = shopRepository.save(Shop.builder()
                .name("Rahul Groceries")
                .pointsBalance(150)
                .build());

        anitha = userRepository.save(User.builder()
                .email("anitha_iso_" + System.currentTimeMillis() + "@test.com")
                .password("secret")
                .fullName("Anitha")
                .role(Role.ROLE_SHOPKEEPER)
                .shop(anithaShop)
                .build());

        rahul = userRepository.save(User.builder()
                .email("rahul_iso_" + System.currentTimeMillis() + "@test.com")
                .password("secret")
                .fullName("Rahul")
                .role(Role.ROLE_SHOPKEEPER)
                .shop(rahulShop)
                .build());

        rahulStaff = userRepository.save(User.builder()
                .email("rahul_staff_" + System.currentTimeMillis() + "@test.com")
                .password("secret")
                .fullName("Rahul Staff")
                .role(Role.ROLE_COUNTER_STAFF)
                .shop(rahulShop)
                .build());

        anithaOffer = offerRepository.save(Offer.builder()
                .shop(anithaShop)
                .title("Anitha's Silk Saree Offer")
                .discountType("PERCENTAGE")
                .discountValue(BigDecimal.valueOf(25))
                .status(OfferStatus.ACTIVE)
                .endDate(LocalDateTime.now().plusDays(10))
                .build());
    }

    @Test
    @DisplayName("Tenant Isolation: Rahul next door must not be able to toggle Anitha's offer status")
    @Transactional
    void testRahulCannotToggleAnithaOffer() {
        UserPrincipal rahulPrincipal = UserPrincipal.create(rahul);

        assertThrows(UnauthorizedShopAccessException.class, () -> {
            offerService.updateOfferStatus(anithaOffer.getId(), OfferStatus.INACTIVE, rahulPrincipal);
        });

        // Verify status remains unchanged
        Offer fetched = offerRepository.findById(anithaOffer.getId()).orElseThrow();
        assertEquals(OfferStatus.ACTIVE, fetched.getStatus());
    }

    @Test
    @DisplayName("Tenant Isolation: Rahul next door must not be able to modify Anitha's offer details")
    @Transactional
    void testRahulCannotModifyAnithaOffer() {
        UserPrincipal rahulPrincipal = UserPrincipal.create(rahul);

        CreateOfferRequest updateReq = CreateOfferRequest.builder()
                .title("Hacked Title")
                .discountType("PERCENTAGE")
                .discountValue(BigDecimal.valueOf(99))
                .build();

        assertThrows(UnauthorizedShopAccessException.class, () -> {
            offerService.updateOffer(anithaOffer.getId(), updateReq, rahulPrincipal);
        });
    }

    @Test
    @DisplayName("Tenant Isolation: Rahul's counter staff cannot redeem an offer voucher claimed from Anitha's shop")
    @Transactional
    void testRahulStaffCannotRedeemAnithaClaim() {
        User shopper = userRepository.save(User.builder()
                .email("shopper_iso_" + System.currentTimeMillis() + "@test.com")
                .password("secret")
                .fullName("Shopper")
                .role(Role.ROLE_SHOPPER)
                .build());

        Claim claim = claimRepository.save(Claim.builder()
                .claimCode("NBR-CROSS-01")
                .offer(anithaOffer)
                .shop(anithaShop)
                .shopper(shopper)
                .status(ClaimStatus.CLAIMED)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build());

        UserPrincipal rahulStaffPrincipal = UserPrincipal.create(rahulStaff);

        RedeemRequest request = RedeemRequest.builder()
                .claimCode("NBR-CROSS-01")
                .build();

        assertThrows(UnauthorizedShopAccessException.class, () -> {
            redemptionService.redeemClaim(request, rahulStaffPrincipal);
        });
    }
}
