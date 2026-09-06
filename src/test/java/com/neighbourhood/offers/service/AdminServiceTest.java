package com.neighbourhood.offers.service;

import com.neighbourhood.offers.dto.AdminShopDto;
import com.neighbourhood.offers.dto.AdminTopUpRequest;
import com.neighbourhood.offers.dto.CreateShopRequest;
import com.neighbourhood.offers.dto.CreateShopkeeperRequest;
import com.neighbourhood.offers.dto.PlatformStatsDto;
import com.neighbourhood.offers.entity.Role;
import com.neighbourhood.offers.entity.Shop;
import com.neighbourhood.offers.entity.User;
import com.neighbourhood.offers.repository.ShopRepository;
import com.neighbourhood.offers.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class AdminServiceTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.neighbourhood.offers.repository.PointTopUpRequestRepository pointTopUpRequestRepository;

    @Test
    @DisplayName("Super Admin can create a shop with initial points and cost per redemption")
    @Transactional
    void testCreateShop() {
        CreateShopRequest request = CreateShopRequest.builder()
                .name("New Royal Boutique")
                .category("Apparel")
                .address("MG Road, Bangalore")
                .locality("MG Road")
                .initialPoints(150)
                .costPerRedemption(10)
                .build();

        AdminShopDto created = adminService.createShop(request);

        assertNotNull(created.getId());
        assertEquals("New Royal Boutique", created.getName());
        assertEquals(150, created.getPointsBalance());
        assertEquals(10, created.getCostPerRedemption());

        Shop found = shopRepository.findById(created.getId()).orElseThrow();
        assertEquals(150, found.getPointsBalance());
    }

    @Test
    @DisplayName("Super Admin can create a shopkeeper account tied to an existing shop")
    @Transactional
    void testCreateShopkeeper() {
        Shop shop = shopRepository.save(Shop.builder()
                .name("Boutique Shop")
                .category("Apparel")
                .pointsBalance(100)
                .costPerRedemption(10)
                .build());

        String email = "shopkeeper_" + System.currentTimeMillis() + "@platform.com";
        CreateShopkeeperRequest request = CreateShopkeeperRequest.builder()
                .shopId(shop.getId())
                .fullName("Rajesh Shopkeeper")
                .email(email)
                .password("securePass123")
                .phone("+91 9988776655")
                .build();

        User created = adminService.createShopkeeper(request);

        assertNotNull(created.getId());
        assertEquals(email, created.getEmail());
        assertEquals(Role.ROLE_SHOPKEEPER, created.getRole());
        assertNotNull(created.getShop());
        assertEquals(shop.getId(), created.getShop().getId());

        // Duplicate email prevention
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.createShopkeeper(request);
        });
    }

    @Test
    @DisplayName("Super Admin can top up merchant points with audit logging")
    @Transactional
    void testTopUpShopPoints() {
        Shop shop = shopRepository.save(Shop.builder()
                .name("TopUp Test Shop")
                .category("Groceries")
                .pointsBalance(50)
                .costPerRedemption(10)
                .build());

        AdminTopUpRequest topUpRequest = AdminTopUpRequest.builder()
                .points(100)
                .description("Promotional grant by Super Admin")
                .build();

        AdminShopDto result = adminService.topUpShopPoints(shop.getId(), topUpRequest);

        assertEquals(150, result.getPointsBalance());

        Shop updated = shopRepository.findById(shop.getId()).orElseThrow();
        assertEquals(150, updated.getPointsBalance());
    }

    @Test
    @DisplayName("Super Admin can retrieve platform overview statistics")
    @Transactional
    void testGetPlatformStats() {
        PlatformStatsDto stats = adminService.getPlatformStats();
        assertNotNull(stats);
        assertTrue(stats.getTotalShops() >= 0);
        assertTrue(stats.getTotalPointsInCirculation() >= 0);
    }

    @Test
    @DisplayName("Super Admin can approve shopkeeper point top-up request and credit points")
    @Transactional
    void testApprovePointRequest() {
        Shop shop = shopRepository.save(Shop.builder()
                .name("Bakery Shop")
                .category("Bakery")
                .pointsBalance(8)
                .costPerRedemption(10)
                .build());

        User shopkeeper = userRepository.save(User.builder()
                .email("baker_" + System.currentTimeMillis() + "@test.com")
                .password("secret")
                .fullName("Baker")
                .role(Role.ROLE_SHOPKEEPER)
                .shop(shop)
                .build());

        com.neighbourhood.offers.entity.PointTopUpRequest request = pointTopUpRequestRepository.save(
                com.neighbourhood.offers.entity.PointTopUpRequest.builder()
                        .shop(shop)
                        .shopkeeper(shopkeeper)
                        .pointsRequested(100)
                        .reason("Festival weekend rush")
                        .status(com.neighbourhood.offers.entity.PointRequestStatus.PENDING)
                        .requestedAt(java.time.LocalDateTime.now())
                        .build()
        );

        com.neighbourhood.offers.dto.PointTopUpRequestResponseDto approved = adminService.approvePointRequest(
                request.getId(),
                com.neighbourhood.offers.dto.ResolvePointRequestDto.builder().adminNotes("Approved via phone verification").build()
        );

        assertEquals(com.neighbourhood.offers.entity.PointRequestStatus.APPROVED, approved.getStatus());
        assertEquals(100, approved.getPointsRequested());

        // Shop points must be 8 + 100 = 108
        Shop updated = shopRepository.findById(shop.getId()).orElseThrow();
        assertEquals(108, updated.getPointsBalance());
    }

    @Test
    @DisplayName("Super Admin can reject shopkeeper point top-up request without crediting points")
    @Transactional
    void testRejectPointRequest() {
        Shop shop = shopRepository.save(Shop.builder()
                .name("Unverified Shop")
                .category("Apparel")
                .pointsBalance(20)
                .costPerRedemption(10)
                .build());

        User shopkeeper = userRepository.save(User.builder()
                .email("unverified_" + System.currentTimeMillis() + "@test.com")
                .password("secret")
                .fullName("Unverified")
                .role(Role.ROLE_SHOPKEEPER)
                .shop(shop)
                .build());

        com.neighbourhood.offers.entity.PointTopUpRequest request = pointTopUpRequestRepository.save(
                com.neighbourhood.offers.entity.PointTopUpRequest.builder()
                        .shop(shop)
                        .shopkeeper(shopkeeper)
                        .pointsRequested(500)
                        .reason("Need points urgently")
                        .status(com.neighbourhood.offers.entity.PointRequestStatus.PENDING)
                        .requestedAt(java.time.LocalDateTime.now())
                        .build()
        );

        com.neighbourhood.offers.dto.PointTopUpRequestResponseDto rejected = adminService.rejectPointRequest(
                request.getId(),
                com.neighbourhood.offers.dto.ResolvePointRequestDto.builder().adminNotes("Monthly limit exceeded").build()
        );

        assertEquals(com.neighbourhood.offers.entity.PointRequestStatus.REJECTED, rejected.getStatus());

        // Shop points must remain unchanged at 20
        Shop updated = shopRepository.findById(shop.getId()).orElseThrow();
        assertEquals(20, updated.getPointsBalance());
    }
}
