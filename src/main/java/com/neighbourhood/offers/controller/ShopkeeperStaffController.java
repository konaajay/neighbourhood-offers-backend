package com.neighbourhood.offers.controller;

import com.neighbourhood.offers.dto.CreateStaffRequest;
import com.neighbourhood.offers.dto.StaffDto;
import com.neighbourhood.offers.security.UserPrincipal;
import com.neighbourhood.offers.service.ShopkeeperStaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shopkeeper/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SHOPKEEPER')")
public class ShopkeeperStaffController {

    private final ShopkeeperStaffService staffService;

    @PostMapping
    public ResponseEntity<StaffDto> createStaff(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateStaffRequest request
    ) {
        if (principal.getShopId() == null) {
            throw new IllegalStateException("Current shopkeeper is not associated with any shop");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(staffService.createStaff(principal.getShopId(), request));
    }

    @GetMapping
    public ResponseEntity<List<StaffDto>> getStaff(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getShopId() == null) {
            throw new IllegalStateException("Current shopkeeper is not associated with any shop");
        }
        return ResponseEntity.ok(staffService.getStaffForShop(principal.getShopId()));
    }
}
