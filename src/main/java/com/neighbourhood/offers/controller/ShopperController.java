package com.neighbourhood.offers.controller;

import com.neighbourhood.offers.dto.ClaimResponseDto;
import com.neighbourhood.offers.dto.CreateClaimRequest;
import com.neighbourhood.offers.dto.OfferResponseDto;
import com.neighbourhood.offers.security.UserPrincipal;
import com.neighbourhood.offers.service.ClaimService;
import com.neighbourhood.offers.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shopper")
@RequiredArgsConstructor
public class ShopperController {

    private final OfferService offerService;
    private final ClaimService claimService;

    @GetMapping("/offers")
    public ResponseEntity<List<OfferResponseDto>> getActiveOffers(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(offerService.getActiveOffersForShoppers(category, search));
    }

    @GetMapping("/offers/{id}")
    public ResponseEntity<OfferResponseDto> getOfferById(@PathVariable Long id) {
        return ResponseEntity.ok(offerService.getOfferById(id));
    }

    @PostMapping("/offers/{id}/claim")
    @PreAuthorize("hasRole('SHOPPER')")
    public ResponseEntity<ClaimResponseDto> claimOffer(
            @PathVariable Long id,
            @RequestBody(required = false) CreateClaimRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return new ResponseEntity<>(claimService.claimOffer(id, request, currentUser), HttpStatus.CREATED);
    }

    @GetMapping("/my-claims")
    @PreAuthorize("hasRole('SHOPPER')")
    public ResponseEntity<List<ClaimResponseDto>> getMyClaims(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(claimService.getShopperClaims(currentUser));
    }
}
