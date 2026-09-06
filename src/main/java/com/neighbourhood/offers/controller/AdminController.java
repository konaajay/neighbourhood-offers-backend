package com.neighbourhood.offers.controller;

import com.neighbourhood.offers.dto.*;
import com.neighbourhood.offers.entity.User;
import com.neighbourhood.offers.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/shops")
    public ResponseEntity<AdminShopDto> createShop(@Valid @RequestBody CreateShopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createShop(request));
    }

    @PostMapping("/shopkeepers")
    public ResponseEntity<User> createShopkeeper(@Valid @RequestBody CreateShopkeeperRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createShopkeeper(request));
    }

    @PostMapping("/shops/{id}/topup")
    public ResponseEntity<AdminShopDto> topUpShopPoints(
            @PathVariable Long id,
            @Valid @RequestBody AdminTopUpRequest request
    ) {
        return ResponseEntity.ok(adminService.topUpShopPoints(id, request));
    }

    @GetMapping("/shops")
    public ResponseEntity<List<AdminShopDto>> getAllShops() {
        return ResponseEntity.ok(adminService.getAllShops());
    }

    @GetMapping("/shopkeepers")
    public ResponseEntity<List<User>> getAllShopkeepers() {
        return ResponseEntity.ok(adminService.getAllShopkeepers());
    }

    @GetMapping("/stats")
    public ResponseEntity<PlatformStatsDto> getPlatformStats() {
        return ResponseEntity.ok(adminService.getPlatformStats());
    }

    @GetMapping("/point-requests")
    public ResponseEntity<List<PointTopUpRequestResponseDto>> getPointRequests(
            @RequestParam(required = false) com.neighbourhood.offers.entity.PointRequestStatus status
    ) {
        return ResponseEntity.ok(adminService.getPointRequests(status));
    }

    @PostMapping("/point-requests/{id}/approve")
    public ResponseEntity<PointTopUpRequestResponseDto> approvePointRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ResolvePointRequestDto dto
    ) {
        return ResponseEntity.ok(adminService.approvePointRequest(id, dto));
    }

    @PostMapping("/point-requests/{id}/reject")
    public ResponseEntity<PointTopUpRequestResponseDto> rejectPointRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ResolvePointRequestDto dto
    ) {
        return ResponseEntity.ok(adminService.rejectPointRequest(id, dto));
    }
}
