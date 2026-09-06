package com.neighbourhood.offers.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUpRequest {

    @NotNull(message = "Points amount is required")
    @Positive(message = "Points amount must be greater than zero")
    private Integer points;

    private BigDecimal amountPaid;

    private String paymentReference;
}
