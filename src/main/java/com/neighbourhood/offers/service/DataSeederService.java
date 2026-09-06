package com.neighbourhood.offers.service;

import com.neighbourhood.offers.entity.*;
import com.neighbourhood.offers.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSeederService implements CommandLineRunner {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final OfferRepository offerRepository;
    private final ClaimRepository claimRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PointTopUpRequestRepository pointTopUpRequestRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains data, checking products seeder...");
            seedProductsIfEmpty();
            return;
        }

        log.info("Seeding realistic neighbourhood retail data...");

        // 1. Create Shops
        Shop anithaShop = Shop.builder()
                .name("Anitha Silks & Sarees")
                .category("Apparel & Ethnic Wear")
                .address("Shop 14, Commercial Street, Shivajinagar")
                .locality("Commercial Street, Bangalore")
                .pointsBalance(100)
                .costPerRedemption(10)
                .build();

        Shop rahulShop = Shop.builder()
                .name("Rahul Organic Groceries & Kirana")
                .category("Groceries & Daily Essentials")
                .address("Plot 42, 11th Main, 4th Block Jayanagar")
                .locality("Jayanagar, Bangalore")
                .pointsBalance(120)
                .costPerRedemption(10)
                .build();

        Shop meenaShop = Shop.builder()
                .name("Meena Bakeries & Cafe")
                .category("Bakery & Confectionery")
                .address("108, 100 Feet Road, Indiranagar")
                .locality("Indiranagar, Bangalore")
                .pointsBalance(8) // Critically low points balance! (< 10)
                .costPerRedemption(10)
                .build();

        Shop sharmaShop = Shop.builder()
                .name("Sharmaji Electronics & Gadgets")
                .category("Electronics & Accessories")
                .address("77, SP Road, City Market")
                .locality("SP Road, Bangalore")
                .pointsBalance(220)
                .costPerRedemption(10)
                .build();

        shopRepository.saveAll(List.of(anithaShop, rahulShop, meenaShop, sharmaShop));

        // 2. Create Users
        String encodedPass = passwordEncoder.encode("password123");

        User anitha = User.builder()
                .email("anitha@shop.com")
                .password(encodedPass)
                .fullName("Anitha Devi")
                .phone("+91 98450 11223")
                .role(Role.ROLE_SHOPKEEPER)
                .shop(anithaShop)
                .build();

        User rahul = User.builder()
                .email("rahul@shop.com")
                .password(encodedPass)
                .fullName("Rahul Verma")
                .phone("+91 98451 22334")
                .role(Role.ROLE_SHOPKEEPER)
                .shop(rahulShop)
                .build();

        User meena = User.builder()
                .email("meena@shop.com")
                .password(encodedPass)
                .fullName("Meena Joseph")
                .phone("+91 98452 33445")
                .role(Role.ROLE_SHOPKEEPER)
                .shop(meenaShop)
                .build();

        User sharma = User.builder()
                .email("sharmaji@shop.com")
                .password(encodedPass)
                .fullName("Ramesh Sharma")
                .phone("+91 98453 44556")
                .role(Role.ROLE_SHOPKEEPER)
                .shop(sharmaShop)
                .build();

        User deepaStaff = User.builder()
                .email("deepa@counter.com")
                .password(encodedPass)
                .fullName("Deepa Nair")
                .phone("+91 98454 55667")
                .role(Role.ROLE_COUNTER_STAFF)
                .shop(anithaShop)
                .build();

        User amitStaff = User.builder()
                .email("amit@counter.com")
                .password(encodedPass)
                .fullName("Amit Kulkarni")
                .phone("+91 98455 66778")
                .role(Role.ROLE_COUNTER_STAFF)
                .shop(rahulShop)
                .build();

        User priyaShopper = User.builder()
                .email("priya@shopper.com")
                .password(encodedPass)
                .fullName("Priya Sharma")
                .phone("+91 98456 77889")
                .role(Role.ROLE_SHOPPER)
                .shop(null)
                .build();

        User vikramShopper = User.builder()
                .email("vikram@shopper.com")
                .password(encodedPass)
                .fullName("Vikram Patel")
                .phone("+91 98457 88990")
                .role(Role.ROLE_SHOPPER)
                .shop(null)
                .build();

        User superAdmin = User.builder()
                .email("admin@platform.com")
                .password(encodedPass)
                .fullName("Platform Super Admin")
                .phone("+91 99999 00000")
                .role(Role.ROLE_SUPER_ADMIN)
                .shop(null)
                .build();

        userRepository.saveAll(List.of(anitha, rahul, meena, sharma, deepaStaff, amitStaff, priyaShopper, vikramShopper, superAdmin));

        LocalDateTime now = LocalDateTime.now();

        // 3. Create Real-World Offers with messy shopkeeper inputs
        Offer offer1 = Offer.builder()
                .shop(anithaShop)
                .title("Flat 20% Off All Sarees")
                .description("Diwali special: Flat 20% discount across Kanjeevaram, Banarasi & Silk sarees.")
                .discountType("PERCENTAGE")
                .discountValue(BigDecimal.valueOf(20))
                .minBillAmount(BigDecimal.valueOf(1500))
                .maxDiscountAmount(BigDecimal.valueOf(2000))
                .applicableCategory("Apparel & Sarees")
                .startDate(now.minusDays(5))
                .endDate(now.plusDays(40))
                .claimLimit(100)
                .claimsCount(24)
                .redemptionCount(12)
                .status(OfferStatus.ACTIVE)
                .rawAiPrompt("flat 20% off all sarees till diwali, min bill 1500")
                .build();

        Offer offer2 = Offer.builder()
                .shop(anithaShop)
                .title("Buy 2 Designer Kurtis Get 1 Free")
                .description("Special weekend offer on all festive cotton and rayon kurtis.")
                .discountType("BOGO")
                .discountValue(BigDecimal.ONE)
                .minBillAmount(BigDecimal.valueOf(999))
                .applicableCategory("Apparel & Sarees")
                .startDate(now.minusDays(2))
                .endDate(now.plusDays(20))
                .claimLimit(50)
                .claimsCount(15)
                .redemptionCount(5)
                .status(OfferStatus.ACTIVE)
                .rawAiPrompt("buy 2 designer kurtis get 1 free on festive stock this month")
                .build();

        Offer offer3 = Offer.builder()
                .shop(rahulShop)
                .title("Save ₹150 on Basmati Rice & Cooking Oils")
                .description("Flat ₹150 cash discount on premium pantry staples with billing above ₹2000.")
                .discountType("FLAT_AMOUNT")
                .discountValue(BigDecimal.valueOf(150))
                .minBillAmount(BigDecimal.valueOf(2000))
                .applicableCategory("Groceries & Staples")
                .startDate(now.minusDays(7))
                .endDate(now.plusDays(25))
                .claimLimit(80)
                .claimsCount(19)
                .redemptionCount(8)
                .status(OfferStatus.ACTIVE)
                .rawAiPrompt("flat 150 rs off on basmati rice and cooking oils order above 2000")
                .build();

        Offer offer4 = Offer.builder()
                .shop(rahulShop)
                .title("10% Off All Organic Spices & Pulses")
                .description("Fresh chemical-free spices and organic pulses directly from farm collectives.")
                .discountType("PERCENTAGE")
                .discountValue(BigDecimal.valueOf(10))
                .minBillAmount(BigDecimal.valueOf(500))
                .applicableCategory("Groceries & Staples")
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(15))
                .claimLimit(60)
                .claimsCount(8)
                .redemptionCount(3)
                .status(OfferStatus.ACTIVE)
                .rawAiPrompt("10% off pure organic spices and pulses weekend rush")
                .build();

        Offer offer5 = Offer.builder()
                .shop(meenaShop)
                .title("Flat 30% Off All Birthday Cakes & Pastries")
                .description("Freshly baked Dutch Truffle, Red Velvet & Fruit gateaux.")
                .discountType("PERCENTAGE")
                .discountValue(BigDecimal.valueOf(30))
                .minBillAmount(BigDecimal.valueOf(600))
                .applicableCategory("Bakery & Desserts")
                .startDate(now.minusDays(3))
                .endDate(now.plusDays(10))
                .claimLimit(40)
                .claimsCount(12)
                .redemptionCount(4)
                .status(OfferStatus.ACTIVE)
                .rawAiPrompt("flat 30% off all birthday cakes and fruit pastries till sunday, min order 600")
                .build();

        Offer offer6 = Offer.builder()
                .shop(sharmaShop)
                .title("Save ₹500 on Audio & Bluetooth Headphones")
                .description("Instant ₹500 off on branded wireless earphones and bluetooth speakers.")
                .discountType("FLAT_AMOUNT")
                .discountValue(BigDecimal.valueOf(500))
                .minBillAmount(BigDecimal.valueOf(2500))
                .applicableCategory("Electronics & Gadgets")
                .startDate(now.minusDays(4))
                .endDate(now.plusDays(30))
                .claimLimit(50)
                .claimsCount(14)
                .redemptionCount(6)
                .status(OfferStatus.ACTIVE)
                .rawAiPrompt("save 500 on all bluetooth speakers and headphones min bill 2500")
                .build();

        Offer expiredOffer = Offer.builder()
                .shop(anithaShop)
                .title("Monsoon Clearance: Flat 50% Off Silk Dupattas")
                .description("End of season monsoon clearance on pure silk dupattas.")
                .discountType("PERCENTAGE")
                .discountValue(BigDecimal.valueOf(50))
                .minBillAmount(BigDecimal.valueOf(1000))
                .applicableCategory("Apparel & Sarees")
                .startDate(now.minusDays(30))
                .endDate(now.minusDays(2)) // EXPIRED
                .claimLimit(20)
                .claimsCount(20)
                .redemptionCount(15)
                .status(OfferStatus.EXPIRED)
                .rawAiPrompt("flat 50% clearance on all silk dupattas till august end")
                .build();

        offerRepository.saveAll(List.of(offer1, offer2, offer3, offer4, offer5, offer6, expiredOffer));

        // 4. Initial Top-ups in point_transactions
        PointTransaction ptAnithaTopUp = PointTransaction.builder()
                .shop(anithaShop)
                .transactionType(TransactionType.TOPUP)
                .pointsAmount(100)
                .balanceAfter(100)
                .description("Initial Merchant Onboarding Points Top-Up")
                .createdAt(now.minusDays(20))
                .build();
        pointTransactionRepository.save(ptAnithaTopUp);

        // 5. Seed Claims for Shopper Priya Sharma (Demonstrating Active, Redeemed, and Expired states)
        Claim priyaRedeemedClaim = Claim.builder()
                .claimCode("NBR-8291-K7")
                .offer(offer1)
                .shop(anithaShop)
                .shopper(priyaShopper)
                .status(ClaimStatus.REDEEMED)
                .claimedAt(now.minusDays(3))
                .expiresAt(now.plusDays(30))
                .redeemedAt(now.minusDays(1))
                .redeemedByStaff(deepaStaff)
                .idempotencyKey("seed-idem-key-8291")
                .billAmountEntered(BigDecimal.valueOf(2450.0))
                .build();

        Claim priyaActiveClaim = Claim.builder()
                .claimCode("NBR-4172-M9")
                .offer(offer2)
                .shop(anithaShop)
                .shopper(priyaShopper)
                .status(ClaimStatus.CLAIMED)
                .claimedAt(now.minusHours(4))
                .expiresAt(now.plusDays(3))
                .build();

        Claim priyaGroceryClaim = Claim.builder()
                .claimCode("NBR-6395-P2")
                .offer(offer3)
                .shop(rahulShop)
                .shopper(priyaShopper)
                .status(ClaimStatus.CLAIMED)
                .claimedAt(now.minusHours(8))
                .expiresAt(now.plusDays(3))
                .build();

        Claim priyaExpiredClaim = Claim.builder()
                .claimCode("NBR-1093-X1")
                .offer(expiredOffer)
                .shop(anithaShop)
                .shopper(priyaShopper)
                .status(ClaimStatus.EXPIRED)
                .claimedAt(now.minusDays(10))
                .expiresAt(now.minusDays(2))
                .build();

        claimRepository.saveAll(List.of(priyaRedeemedClaim, priyaActiveClaim, priyaGroceryClaim, priyaExpiredClaim));

        // 6. Record transaction for priyaRedeemedClaim
        PointTransaction ptPriya = PointTransaction.builder()
                .shop(anithaShop)
                .claim(priyaRedeemedClaim)
                .transactionType(TransactionType.REDEMPTION_DEBIT)
                .pointsAmount(-10)
                .balanceAfter(240)
                .description("Redemption of claim NBR-8291-K7 for 'Flat 20% Off All Sarees'")
                .createdAt(now.minusDays(1))
                .build();
        pointTransactionRepository.save(ptPriya);

        // 7. Seed historical daily redemptions in current month for Anitha Silks to provide rich analytics (Busy Days)
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        int currentDay = LocalDate.now().getDayOfMonth();

        int runningBalance = 240;
        for (int day = 1; day < currentDay; day++) {
            LocalDate date = monthStart.plusDays(day - 1);
            // Simulate realistic shopping patterns: higher footfall on Friday, Saturday, Sunday
            int footfall = switch (date.getDayOfWeek()) {
                case SATURDAY, SUNDAY -> 4;
                case FRIDAY -> 3;
                default -> 1;
            };

            for (int k = 0; k < footfall; k++) {
                LocalDateTime redeemTime = date.atTime(11 + (k * 2), 15 + (k * 10));
                Claim histClaim = Claim.builder()
                        .claimCode("NBR-H" + day + "K" + k + "-Z")
                        .offer(offer1)
                        .shop(anithaShop)
                        .shopper(vikramShopper)
                        .status(ClaimStatus.REDEEMED)
                        .claimedAt(redeemTime.minusHours(3))
                        .expiresAt(redeemTime.plusDays(3))
                        .redeemedAt(redeemTime)
                        .redeemedByStaff(deepaStaff)
                        .billAmountEntered(BigDecimal.valueOf(1800 + (k * 350)))
                        .build();
                claimRepository.save(histClaim);

                runningBalance -= 10;
                PointTransaction pt = PointTransaction.builder()
                        .shop(anithaShop)
                        .claim(histClaim)
                        .transactionType(TransactionType.REDEMPTION_DEBIT)
                        .pointsAmount(-10)
                        .balanceAfter(Math.max(runningBalance, 100))
                        .description("Historical footfall redemption on " + date)
                        .createdAt(redeemTime)
                        .build();
                pointTransactionRepository.save(pt);
            }
        }

        // 8. Seed a pending point top-up request from Meena (Low points)
        PointTopUpRequest meenaRequest = PointTopUpRequest.builder()
                .shop(meenaShop)
                .shopkeeper(meena)
                .pointsRequested(100)
                .reason("Weekend festival rush, depleted balance (8 pts left). Please approve top-up.")
                .status(PointRequestStatus.PENDING)
                .requestedAt(now.minusHours(2))
                .build();
        pointTopUpRequestRepository.save(meenaRequest);

        log.info("Realistic neighbourhood seed data successfully loaded!");
        seedProductsIfEmpty();
    }

    private void seedProductsIfEmpty() {
        if (userRepository.findByEmail("admin@platform.com").isEmpty()) {
            userRepository.save(User.builder()
                    .email("admin@platform.com")
                    .password(passwordEncoder.encode("password123"))
                    .fullName("Platform Super Admin")
                    .phone("+91 99999 00000")
                    .role(Role.ROLE_SUPER_ADMIN)
                    .shop(null)
                    .build());
            log.info("Ensured super admin user exists.");
        }

        if (productRepository.count() > 0) {
            return;
        }
        log.info("Seeding realistic products for offers...");
        List<Offer> offers = offerRepository.findAll();
        for (Offer offer : offers) {
            String title = offer.getTitle().toLowerCase();
            if (title.contains("saree")) {
                productRepository.save(Product.builder()
                        .offer(offer)
                        .name("Kanjeevaram Silk Saree - Crimson Gold")
                        .description("Pure zari woven traditional bridal Kanjeevaram silk saree.")
                        .price(BigDecimal.valueOf(2499.00))
                        .imageUrl("https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=600&auto=format&fit=crop&q=80")
                        .build());
                productRepository.save(Product.builder()
                        .offer(offer)
                        .name("Banarasi Georgette Saree - Royal Teal")
                        .description("Intricate handloom floral jaal border with rich pallu.")
                        .price(BigDecimal.valueOf(1850.00))
                        .imageUrl("https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?w=600&auto=format&fit=crop&q=80")
                        .build());
                productRepository.save(Product.builder()
                        .offer(offer)
                        .name("Chanderi Cotton Silk Saree - Blush Rose")
                        .description("Lightweight breezy festive drape with contrast zari pallu.")
                        .price(BigDecimal.valueOf(1250.00))
                        .imageUrl("https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=600&auto=format&fit=crop&q=80")
                        .build());
            } else if (title.contains("kurti")) {
                productRepository.save(Product.builder()
                        .offer(offer)
                        .name("Hand-Block Printed Anarkali Kurti")
                        .description("Pure cotton Jaipur block print with flared hem.")
                        .price(BigDecimal.valueOf(699.00))
                        .imageUrl("https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=600&auto=format&fit=crop&q=80")
                        .build());
                productRepository.save(Product.builder()
                        .offer(offer)
                        .name("Embroidered Festive Rayon Kurti Set")
                        .description("With matching palazzo pants and chiffon dupatta.")
                        .price(BigDecimal.valueOf(999.00))
                        .imageUrl("https://images.unsplash.com/photo-1585487000160-6ebcfceb0d03?w=600&auto=format&fit=crop&q=80")
                        .build());
            } else if (title.contains("basmati") || title.contains("groceries")) {
                productRepository.save(Product.builder()
                        .offer(offer)
                        .name("Royal Kohinoor Aged Basmati Rice 5kg")
                        .description("Extra-long grain fragrant basmati aged 2 years.")
                        .price(BigDecimal.valueOf(850.00))
                        .imageUrl("https://images.unsplash.com/photo-1586201375761-83865001e31c?w=600&auto=format&fit=crop&q=80")
                        .build());
                productRepository.save(Product.builder()
                        .offer(offer)
                        .name("Cold-Pressed Organic Groundnut Oil 5L")
                        .description("Traditional wood-pressed unfiltered cooking oil.")
                        .price(BigDecimal.valueOf(1199.00))
                        .imageUrl("https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=600&auto=format&fit=crop&q=80")
                        .build());
            } else if (title.contains("cake") || title.contains("pastries")) {
                productRepository.save(Product.builder()
                        .offer(offer)
                        .name("Belgian Dark Chocolate Truffle Cake 1kg")
                        .description("Rich Belgian ganache layered with moist cocoa sponge.")
                        .price(BigDecimal.valueOf(750.00))
                        .imageUrl("https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=600&auto=format&fit=crop&q=80")
                        .build());
                productRepository.save(Product.builder()
                        .offer(offer)
                        .name("Red Velvet Cream Cheese Gateau 1kg")
                        .description("Classic crimson crumb with genuine cream cheese frosting.")
                        .price(BigDecimal.valueOf(850.00))
                        .imageUrl("https://images.unsplash.com/photo-1586788680434-30d324b2d46f?w=600&auto=format&fit=crop&q=80")
                        .build());
            }
        }
        log.info("Products seeded successfully.");
    }
}
