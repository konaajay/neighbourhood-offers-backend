package com.neighbourhood.offers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClaimRequest {
    private String cartSnapshotJson;
    private BigDecimal estimatedTotal;
}
