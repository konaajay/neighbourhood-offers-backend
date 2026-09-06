package com.neighbourhood.offers.controller;

import com.neighbourhood.offers.dto.LookupClaimResponseDto;
import com.neighbourhood.offers.dto.RedeemRequest;
import com.neighbourhood.offers.dto.RedemptionReceiptDto;
import com.neighbourhood.offers.security.UserPrincipal;
import com.neighbourhood.offers.service.RedemptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/counter")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COUNTER_STAFF', 'SHOPKEEPER')")
public class CounterController {

    private final RedemptionService redemptionService;

    @PostMapping("/lookup")
    public ResponseEntity<LookupClaimResponseDto> lookupClaim(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal staff) {
        String claimCode = body.get("claimCode");
        return ResponseEntity.ok(redemptionService.lookupClaim(claimCode, staff));
    }

    @PostMapping("/redeem")
    public ResponseEntity<RedemptionReceiptDto> redeemClaim(
            @Valid @RequestBody RedeemRequest request,
            @AuthenticationPrincipal UserPrincipal staff) {
        return ResponseEntity.ok(redemptionService.redeemClaim(request, staff));
    }
}
