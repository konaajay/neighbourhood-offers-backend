package com.neighbourhood.offers.service;

import com.neighbourhood.offers.entity.Role;
import com.neighbourhood.offers.entity.User;
import com.neighbourhood.offers.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserActiveTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("User entity defaults active to true via builder")
    void testUserBuilderDefaultsActiveToTrue() {
        User user = User.builder()
                .email("test-default@shop.com")
                .password("encodedPassword")
                .fullName("Test User")
                .role(Role.ROLE_SHOPPER)
                .build();

        assertNotNull(user.getActive());
        assertTrue(user.getActive());
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("User entity saved without explicit active column persists with active=true")
    void testUserSavedPersistsActiveTrue() {
        User user = User.builder()
                .email("test-persist@shop.com")
                .password("encodedPassword")
                .fullName("Persist User")
                .role(Role.ROLE_SHOPPER)
                .build();

        User saved = userRepository.save(user);
        assertNotNull(saved.getId());
        assertNotNull(saved.getActive());
        assertTrue(saved.getActive());
        assertTrue(saved.isActive());

        User found = userRepository.findById(saved.getId()).orElseThrow();
        assertEquals(Boolean.TRUE, found.getActive());
    }

    @Test
    @DisplayName("PrePersist ensures active is never null even if forced to null")
    void testPrePersistEnforcesNonNullActive() {
        User user = new User();
        user.setEmail("test-null@shop.com");
        user.setPassword("encodedPassword");
        user.setFullName("Forced Null User");
        user.setRole(Role.ROLE_SHOPPER);
        user.setActive(null);

        User saved = userRepository.save(user);
        assertNotNull(saved.getActive());
        assertTrue(saved.getActive());
    }
}
