package com.neighbourhood.offers.service;

import com.neighbourhood.offers.dto.CreateProductRequest;
import com.neighbourhood.offers.dto.ProductDto;
import com.neighbourhood.offers.entity.*;
import com.neighbourhood.offers.exception.UnauthorizedShopAccessException;
import com.neighbourhood.offers.repository.OfferRepository;
import com.neighbourhood.offers.repository.ProductRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private UserRepository userRepository;

    private Shop anithaShop;
    private Shop rahulShop;
    private User anitha;
    private User rahul;
    private UserPrincipal anithaPrincipal;
    private UserPrincipal rahulPrincipal;
    private UserPrincipal deepaStaffPrincipal;
    private Offer anithaOffer;

    @BeforeEach
    void setUp() {
        anithaShop = shopRepository.save(Shop.builder().name("Anitha Sarees").pointsBalance(100).build());
        rahulShop = shopRepository.save(Shop.builder().name("Rahul Groceries").pointsBalance(100).build());

        anitha = userRepository.save(User.builder()
                .email("anitha_prod_" + System.currentTimeMillis() + "@test.com")
                .password("pass")
                .fullName("Anitha Devi")
                .role(Role.ROLE_SHOPKEEPER)
                .shop(anithaShop)
                .build());

        rahul = userRepository.save(User.builder()
                .email("rahul_prod_" + System.currentTimeMillis() + "@test.com")
                .password("pass")
                .fullName("Rahul Verma")
                .role(Role.ROLE_SHOPKEEPER)
                .shop(rahulShop)
                .build());

        anithaPrincipal = UserPrincipal.create(anitha);
        rahulPrincipal = UserPrincipal.create(rahul);

        User deepaStaff = userRepository.save(User.builder()
                .email("deepa_staff_" + System.currentTimeMillis() + "@test.com")
                .password("pass")
                .fullName("Deepa Counter")
                .role(Role.ROLE_COUNTER_STAFF)
                .shop(anithaShop)
                .build());
        deepaStaffPrincipal = UserPrincipal.create(deepaStaff);

        anithaOffer = offerRepository.save(Offer.builder()
                .shop(anithaShop)
                .title("20% Off Silk Sarees")
                .discountType("PERCENTAGE")
                .discountValue(BigDecimal.valueOf(20))
                .minBillAmount(BigDecimal.valueOf(1500))
                .status(OfferStatus.ACTIVE)
                .endDate(LocalDateTime.now().plusDays(10))
                .build());
    }

    @Test
    @DisplayName("Shopkeeper can add products to their own offer")
    @Transactional
    void testAddProductToOwnOffer() {
        CreateProductRequest req = CreateProductRequest.builder()
                .name("Kanjeevaram Silk Saree")
                .description("Crimson gold pure zari saree")
                .price(BigDecimal.valueOf(2499.00))
                .imageUrl("https://example.com/saree.jpg")
                .build();

        ProductDto created = productService.addProductToOffer(anithaOffer.getId(), req, anithaPrincipal);

        assertNotNull(created.getId());
        assertEquals("Kanjeevaram Silk Saree", created.getName());
        assertEquals(BigDecimal.valueOf(2499.00), created.getPrice());
        assertEquals("https://example.com/saree.jpg", created.getImageUrl());

        List<ProductDto> products = productService.getProductsByOffer(anithaOffer.getId());
        assertEquals(1, products.size());
    }

    @Test
    @DisplayName("Tenant Isolation: Rahul cannot add product to Anitha's offer")
    @Transactional
    void testTenantIsolationOnAddProduct() {
        CreateProductRequest req = CreateProductRequest.builder()
                .name("Unauthorized Product")
                .price(BigDecimal.valueOf(500.00))
                .build();

        assertThrows(UnauthorizedShopAccessException.class, () -> {
            productService.addProductToOffer(anithaOffer.getId(), req, rahulPrincipal);
        });
    }

    @Test
    @DisplayName("Tenant Isolation: Rahul cannot update or delete Anitha's product")
    @Transactional
    void testTenantIsolationOnUpdateAndDeleteProduct() {
        Product prod = productRepository.save(Product.builder()
                .offer(anithaOffer)
                .name("Banarasi Saree")
                .price(BigDecimal.valueOf(1800.00))
                .build());

        CreateProductRequest updateReq = CreateProductRequest.builder()
                .name("Hacked Saree")
                .price(BigDecimal.valueOf(1.00))
                .build();

        assertThrows(UnauthorizedShopAccessException.class, () -> {
            productService.updateProduct(prod.getId(), updateReq, rahulPrincipal);
        });

        assertThrows(UnauthorizedShopAccessException.class, () -> {
            productService.deleteProduct(prod.getId(), rahulPrincipal);
        });
    }

    @Test
    @DisplayName("Role Separation: Counter Staff cannot add product (read-only restriction)")
    @Transactional
    void testCounterStaffCannotAddProduct() {
        CreateProductRequest req = CreateProductRequest.builder()
                .name("Unauthorized Staff Product")
                .price(BigDecimal.valueOf(999.00))
                .build();

        UnauthorizedShopAccessException ex = assertThrows(UnauthorizedShopAccessException.class, () -> {
            productService.addProductToOffer(anithaOffer.getId(), req, deepaStaffPrincipal);
        });
        assertTrue(ex.getMessage().contains("read-only"));
    }

    @Test
    @DisplayName("Role Separation: Counter Staff cannot update or delete product")
    @Transactional
    void testCounterStaffCannotUpdateAndDeleteProduct() {
        Product prod = productRepository.save(Product.builder()
                .offer(anithaOffer)
                .name("Chanderi Saree")
                .price(BigDecimal.valueOf(1250.00))
                .build());

        CreateProductRequest updateReq = CreateProductRequest.builder()
                .name("Staff Price Override")
                .price(BigDecimal.valueOf(100.00))
                .build();

        assertThrows(UnauthorizedShopAccessException.class, () -> {
            productService.updateProduct(prod.getId(), updateReq, deepaStaffPrincipal);
        });

        assertThrows(UnauthorizedShopAccessException.class, () -> {
            productService.deleteProduct(prod.getId(), deepaStaffPrincipal);
        });
    }

    @Test
    @DisplayName("Role Separation: Counter Staff CAN read products of offer")
    @Transactional
    void testCounterStaffCanReadProducts() {
        productRepository.save(Product.builder()
                .offer(anithaOffer)
                .name("Chanderi Saree")
                .price(BigDecimal.valueOf(1250.00))
                .build());

        List<ProductDto> products = productService.getProductsByOffer(anithaOffer.getId());
        assertNotNull(products);
        assertFalse(products.isEmpty());
        assertEquals("Chanderi Saree", products.get(0).getName());
    }

    @Test
    @DisplayName("Role Separation: Counter Staff CAN read products of shop")
    @Transactional
    void testCounterStaffCanReadShopProducts() {
        productRepository.save(Product.builder()
                .offer(anithaOffer)
                .name("Chanderi Saree")
                .price(BigDecimal.valueOf(1250.00))
                .build());

        List<ProductDto> shopProducts = productService.getProductsByShop(anithaShop.getId());
        assertNotNull(shopProducts);
        assertFalse(shopProducts.isEmpty());
        assertEquals("Chanderi Saree", shopProducts.get(0).getName());
    }
}
