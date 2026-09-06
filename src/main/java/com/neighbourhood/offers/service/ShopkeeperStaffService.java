package com.neighbourhood.offers.service;

import com.neighbourhood.offers.dto.CreateStaffRequest;
import com.neighbourhood.offers.dto.StaffDto;
import com.neighbourhood.offers.entity.Role;
import com.neighbourhood.offers.entity.Shop;
import com.neighbourhood.offers.entity.User;
import com.neighbourhood.offers.exception.ResourceNotFoundException;
import com.neighbourhood.offers.repository.ShopRepository;
import com.neighbourhood.offers.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopkeeperStaffService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public StaffDto createStaff(Long shopId, CreateStaffRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with ID: " + shopId));

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }

        User staff = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(Role.ROLE_COUNTER_STAFF)
                .shop(shop)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(staff);
        log.info("Created counter staff member '{}' for shop '{}'", saved.getEmail(), shop.getName());
        return StaffDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<StaffDto> getStaffForShop(Long shopId) {
        return userRepository.findByShopIdAndRole(shopId, Role.ROLE_COUNTER_STAFF).stream()
                .map(StaffDto::fromEntity)
                .collect(Collectors.toList());
    }
}
