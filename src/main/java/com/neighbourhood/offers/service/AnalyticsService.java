package com.neighbourhood.offers.service;

import com.neighbourhood.offers.dto.DailyFootfallDto;
import com.neighbourhood.offers.dto.MonthlyAnalyticsDto;
import com.neighbourhood.offers.entity.Claim;
import com.neighbourhood.offers.entity.PointTransaction;
import com.neighbourhood.offers.entity.Shop;
import com.neighbourhood.offers.entity.TransactionType;
import com.neighbourhood.offers.exception.ResourceNotFoundException;
import com.neighbourhood.offers.exception.UnauthorizedShopAccessException;
import com.neighbourhood.offers.repository.ClaimRepository;
import com.neighbourhood.offers.repository.PointTransactionRepository;
import com.neighbourhood.offers.repository.ShopRepository;
import com.neighbourhood.offers.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ShopRepository shopRepository;
    private final ClaimRepository claimRepository;
    private final PointTransactionRepository pointTransactionRepository;

    @Transactional(readOnly = true)
    public MonthlyAnalyticsDto getMonthlyAnalytics(Integer year, Integer month, UserPrincipal currentUser) {
        if (currentUser.getShopId() == null) {
            throw new UnauthorizedShopAccessException("Current user does not manage a shop");
        }

        Shop shop = shopRepository.findById(currentUser.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        LocalDate today = LocalDate.now();
        int targetYear = (year != null && year > 2000) ? year : today.getYear();
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : today.getMonthValue();

        YearMonth ym = YearMonth.of(targetYear, targetMonth);
        LocalDateTime startOfMonth = ym.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = ym.atEndOfMonth().atTime(23, 59, 59);

        // Fetch transactions for the month
        List<PointTransaction> transactions = pointTransactionRepository.findByShopIdAndPeriod(
                shop.getId(), startOfMonth, endOfMonth);

        int pointsSpent = 0;
        int pointsTopUp = 0;
        for (PointTransaction pt : transactions) {
            if (pt.getTransactionType() == TransactionType.REDEMPTION_DEBIT) {
                pointsSpent += Math.abs(pt.getPointsAmount());
            } else if (pt.getTransactionType() == TransactionType.TOPUP) {
                pointsTopUp += pt.getPointsAmount();
            }
        }

        // Fetch redeemed claims for the month
        List<Claim> redeemedClaims = claimRepository.findRedeemedClaimsInPeriod(
                shop.getId(), startOfMonth, endOfMonth);

        int totalRedemptions = redeemedClaims.size();
        BigDecimal totalSalesValue = redeemedClaims.stream()
                .map(c -> c.getBillAmountEntered() != null ? c.getBillAmountEntered() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group redemptions by day of the month
        Map<LocalDate, Long> redemptionsByDay = redeemedClaims.stream()
                .filter(c -> c.getRedeemedAt() != null)
                .collect(Collectors.groupingBy(c -> c.getRedeemedAt().toLocalDate(), Collectors.counting()));

        List<DailyFootfallDto> dailyFootfall = new ArrayList<>();
        int daysInMonth = ym.lengthOfMonth();
        LocalDate maxDay = null;
        int maxCount = 0;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = ym.atDay(day);
            int count = redemptionsByDay.getOrDefault(date, 0L).intValue();
            int ptsSpent = count * shop.getCostPerRedemption();

            dailyFootfall.add(DailyFootfallDto.builder()
                    .date(date)
                    .dayOfWeek(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .redemptionsCount(count)
                    .pointsSpent(ptsSpent)
                    .build());

            if (count > maxCount) {
                maxCount = count;
                maxDay = date;
            }
        }

        String busiestDayText = maxDay != null 
                ? maxDay.toString() + " (" + maxDay.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ")"
                : "No redemptions yet";

        return MonthlyAnalyticsDto.builder()
                .year(targetYear)
                .month(targetMonth)
                .monthName(ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                .shopId(shop.getId())
                .shopName(shop.getName())
                .pointsSpent(pointsSpent)
                .pointsTopUp(pointsTopUp)
                .currentBalance(shop.getPointsBalance())
                .totalRedemptions(totalRedemptions)
                .totalSalesValue(totalSalesValue)
                .dailyFootfall(dailyFootfall)
                .busiestDay(busiestDayText)
                .busiestDayFootfall(maxCount)
                .build();
    }
}
