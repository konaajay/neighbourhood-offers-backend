package com.neighbourhood.offers.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedeemRequest {

    @NotBlank(message = "Claim code is required")
    private String claimCode;

    private String idempotencyKey;

    private BigDecimal billAmount;
}
