package com.neighbourhood.offers.controller;

import com.neighbourhood.offers.dto.MonthlyAnalyticsDto;
import com.neighbourhood.offers.security.UserPrincipal;
import com.neighbourhood.offers.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shopkeeper/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SHOPKEEPER')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyAnalyticsDto> getMonthlyAnalytics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(analyticsService.getMonthlyAnalytics(year, month, currentUser));
    }
}
