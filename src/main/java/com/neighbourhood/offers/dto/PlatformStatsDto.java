package com.neighbourhood.offers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStatsDto {
    private long totalShops;
    private long totalShopkeepers;
    private long totalPointsInCirculation;
    private long totalRedemptions;
    private long totalOffers;
}
