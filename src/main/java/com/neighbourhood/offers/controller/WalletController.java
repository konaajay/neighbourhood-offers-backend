package com.neighbourhood.offers.controller;

import com.neighbourhood.offers.dto.PointTransactionDto;
import com.neighbourhood.offers.dto.TopUpRequest;
import com.neighbourhood.offers.dto.WalletDto;
import com.neighbourhood.offers.security.UserPrincipal;
import com.neighbourhood.offers.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shopkeeper/wallet")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SHOPKEEPER')")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<WalletDto> getWallet(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(walletService.getWallet(currentUser));
    }

    @PostMapping("/topup")
    public ResponseEntity<WalletDto> topUp(
            @Valid @RequestBody TopUpRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(walletService.topUp(request, currentUser));
    }

    @PostMapping("/request-topup")
    public ResponseEntity<com.neighbourhood.offers.dto.PointTopUpRequestResponseDto> requestTopUp(
            @Valid @RequestBody com.neighbourhood.offers.dto.CreatePointRequestDto request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(walletService.requestPoints(request, currentUser));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<com.neighbourhood.offers.dto.PointTopUpRequestResponseDto>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(walletService.getMyPointRequests(currentUser));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<PointTransactionDto>> getTransactions(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(walletService.getTransactions(currentUser));
    }
}
