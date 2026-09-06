package com.neighbourhood.offers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyAnalyticsDto {
    private Integer year;
    private Integer month;
    private String monthName;
    private Long shopId;
    private String shopName;
    private Integer pointsSpent;
    private Integer pointsTopUp;
    private Integer currentBalance;
    private Integer totalRedemptions;
    private BigDecimal totalSalesValue;
    private List<DailyFootfallDto> dailyFootfall;
    private String busiestDay;
    private Integer busiestDayFootfall;
}
