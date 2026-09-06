package com.neighbourhood.offers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminShopDto {
    private Long id;
    private String name;
    private String category;
    private String address;
    private String locality;
    private Integer pointsBalance;
    private Integer costPerRedemption;
    private String shopkeeperName;
    private String shopkeeperEmail;
    private Long shopkeeperId;
    private Integer activeOffersCount;
    private Integer totalRedemptions;
    private LocalDateTime createdAt;
}
