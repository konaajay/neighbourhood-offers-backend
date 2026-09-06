package com.neighbourhood.offers.service;

import com.neighbourhood.offers.dto.CreateStaffRequest;
import com.neighbourhood.offers.dto.StaffDto;
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
public class ShopkeeperStaffServiceTest {

    @Autowired
    private ShopkeeperStaffService staffService;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Shopkeeper can onboard counter staff for their shop")
    @Transactional
    void testCreateStaff() {
        Shop shop = shopRepository.save(Shop.builder()
                .name("Kavitha Sarees")
                .category("Apparel")
                .pointsBalance(100)
                .costPerRedemption(10)
                .build());

        String email = "staff_" + System.currentTimeMillis() + "@kavitha.com";
        CreateStaffRequest request = CreateStaffRequest.builder()
                .fullName("Sunil Staff")
                .email(email)
                .password("staffPass123")
                .phone("+91 91234 56789")
                .build();

        StaffDto staff = staffService.createStaff(shop.getId(), request);

        assertNotNull(staff.getId());
        assertEquals("Sunil Staff", staff.getFullName());
        assertEquals(email, staff.getEmail());
        assertEquals(shop.getId(), staff.getShopId());

        User found = userRepository.findById(staff.getId()).orElseThrow();
        assertEquals(Role.ROLE_COUNTER_STAFF, found.getRole());
        assertEquals(shop.getId(), found.getShop().getId());

        // Duplicate email rejection
        assertThrows(IllegalArgumentException.class, () -> {
            staffService.createStaff(shop.getId(), request);
        });
    }

    @Test
    @DisplayName("Shopkeeper can list all counter staff for their shop")
    @Transactional
    void testGetStaffForShop() {
        Shop shop = shopRepository.save(Shop.builder()
                .name("Multi-Staff Shop")
                .category("Groceries")
                .pointsBalance(100)
                .costPerRedemption(10)
                .build());

        String email1 = "staff1_" + System.currentTimeMillis() + "@test.com";
        String email2 = "staff2_" + System.currentTimeMillis() + "@test.com";

        staffService.createStaff(shop.getId(), CreateStaffRequest.builder()
                .fullName("Staff One")
                .email(email1)
                .password("pass1")
                .build());

        staffService.createStaff(shop.getId(), CreateStaffRequest.builder()
                .fullName("Staff Two")
                .email(email2)
                .password("pass2")
                .build());

        List<StaffDto> staffList = staffService.getStaffForShop(shop.getId());
        assertEquals(2, staffList.size());
        assertTrue(staffList.stream().anyMatch(s -> s.getEmail().equals(email1)));
        assertTrue(staffList.stream().anyMatch(s -> s.getEmail().equals(email2)));
    }
}
