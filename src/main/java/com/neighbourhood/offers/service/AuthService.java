package com.neighbourhood.offers.service;

import com.neighbourhood.offers.dto.AuthRequest;
import com.neighbourhood.offers.dto.AuthResponse;
import com.neighbourhood.offers.dto.DemoPersonaDto;
import com.neighbourhood.offers.entity.Role;
import com.neighbourhood.offers.entity.User;
import com.neighbourhood.offers.repository.UserRepository;
import com.neighbourhood.offers.security.JwtTokenProvider;
import com.neighbourhood.offers.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Integer points = user.getShop() != null ? user.getShop().getPointsBalance() : null;

        return AuthResponse.builder()
                .token(jwt)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .shopId(user.getShop() != null ? user.getShop().getId() : null)
                .shopName(user.getShop() != null ? user.getShop().getName() : null)
                .pointsBalance(points)
                .build();
    }

    public List<DemoPersonaDto> getDemoPersonas() {
        return Arrays.asList(
                DemoPersonaDto.builder()
                        .email("admin@platform.com")
                        .fullName("Platform Super Admin")
                        .role(Role.ROLE_SUPER_ADMIN)
                        .shopId(null)
                        .shopName(null)
                        .description("Platform administrator: creates shops, provisions shopkeepers, tops up merchant points.")
                        .tag("Super Admin")
                        .color("indigo")
                        .build(),
                DemoPersonaDto.builder()
                        .email("anitha@shop.com")
                        .fullName("Anitha Devi")
                        .role(Role.ROLE_SHOPKEEPER)
                        .shopId(1L)
                        .shopName("Anitha Silks & Sarees")
                        .description("Shopkeeper starting with 100 points & multiple active saree offers.")
                        .tag("Shopkeeper - Active")
                        .color("purple")
                        .build(),
                DemoPersonaDto.builder()
                        .email("rahul@shop.com")
                        .fullName("Rahul Verma")
                        .role(Role.ROLE_SHOPKEEPER)
                        .shopId(2L)
                        .shopName("Rahul Organic Groceries & Kirana")
                        .description("Shopkeeper next door (tests multi-tenant isolation against Anitha).")
                        .tag("Shopkeeper - Tenant Isolation")
                        .color("blue")
                        .build(),
                DemoPersonaDto.builder()
                        .email("meena@shop.com")
                        .fullName("Meena Joseph")
                        .role(Role.ROLE_SHOPKEEPER)
                        .shopId(3L)
                        .shopName("Meena Bakeries & Cafe")
                        .description("Shopkeeper nearly out of points (8 pts left, cost is 10 pts).")
                        .tag("Shopkeeper - Low Points")
                        .color("rose")
                        .build(),
                DemoPersonaDto.builder()
                        .email("deepa@counter.com")
                        .fullName("Deepa Nair")
                        .role(Role.ROLE_COUNTER_STAFF)
                        .shopId(1L)
                        .shopName("Anitha Silks & Sarees")
                        .description("Counter staff at Anitha Silks. Redeems vouchers with idempotency protection.")
                        .tag("Counter - Anitha Silks")
                        .color("amber")
                        .build(),
                DemoPersonaDto.builder()
                        .email("amit@counter.com")
                        .fullName("Amit Kulkarni")
                        .role(Role.ROLE_COUNTER_STAFF)
                        .shopId(2L)
                        .shopName("Rahul Organic Groceries & Kirana")
                        .description("Counter staff at Rahul Groceries (weak cellular connection, tests idempotency & cannot redeem twice).")
                        .tag("Counter - Weak Network")
                        .color("orange")
                        .build(),
                DemoPersonaDto.builder()
                        .email("meenastaff@counter.com")
                        .fullName("Meena Staff")
                        .role(Role.ROLE_COUNTER_STAFF)
                        .shopId(3L)
                        .shopName("Meena Bakeries & Cafe")
                        .description("Counter staff at Meena Bakeries (tests low points rejection: 8 pts vs 10 pts cost).")
                        .tag("Counter - Low Points")
                        .color("rose")
                        .build(),
                DemoPersonaDto.builder()
                        .email("priya@shopper.com")
                        .fullName("Priya Sharma")
                        .role(Role.ROLE_SHOPPER)
                        .shopId(null)
                        .shopName(null)
                        .description("Active shopper with multiple claimed vouchers (Active, Redeemed, Expired).")
                        .tag("Shopper (Claimed Several)")
                        .color("emerald")
                        .build()
        );
    }
}
