package com.neighbourhood.offers.controller;

import com.neighbourhood.offers.dto.AuthRequest;
import com.neighbourhood.offers.dto.AuthResponse;
import com.neighbourhood.offers.dto.DemoPersonaDto;
import com.neighbourhood.offers.security.UserPrincipal;
import com.neighbourhood.offers.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/demo-personas")
    public ResponseEntity<List<DemoPersonaDto>> getDemoPersonas() {
        return ResponseEntity.ok(authService.getDemoPersonas());
    }

    @GetMapping("/me")
    public ResponseEntity<UserPrincipal> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(userPrincipal);
    }
}
