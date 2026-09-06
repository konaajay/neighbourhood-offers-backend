package com.neighbourhood.offers.controller;

import com.neighbourhood.offers.dto.CreateOfferRequest;
import com.neighbourhood.offers.dto.OfferResponseDto;
import com.neighbourhood.offers.dto.UpdateOfferStatusRequest;
import com.neighbourhood.offers.security.UserPrincipal;
import com.neighbourhood.offers.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shopkeeper/offers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SHOPKEEPER')")
public class OfferController {

    private final OfferService offerService;

    @GetMapping
    public ResponseEntity<List<OfferResponseDto>> getOffers(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(offerService.getOffersForShopkeeper(currentUser));
    }

    @PostMapping
    public ResponseEntity<OfferResponseDto> createOffer(
            @Valid @RequestBody CreateOfferRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return new ResponseEntity<>(offerService.createOffer(request, currentUser), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OfferResponseDto> updateOffer(
            @PathVariable Long id,
            @Valid @RequestBody CreateOfferRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(offerService.updateOffer(id, request, currentUser));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OfferResponseDto> updateOfferStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOfferStatusRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(offerService.updateOfferStatus(id, request.getStatus(), currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        offerService.deleteOffer(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
