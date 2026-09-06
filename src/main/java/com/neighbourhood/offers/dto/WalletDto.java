package com.neighbourhood.offers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDto {
    private Long shopId;
    private String shopName;
    private Integer pointsBalance;
    private Integer costPerRedemption;
    private Boolean isLowBalance;
    private Integer lowBalanceThreshold;
}
