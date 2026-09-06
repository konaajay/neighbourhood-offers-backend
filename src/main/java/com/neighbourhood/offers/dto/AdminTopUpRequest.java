package com.neighbourhood.offers.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTopUpRequest {

    @NotNull(message = "Points amount is required")
    @Positive(message = "Points amount must be greater than zero")
    private Integer points;

    private String description;
}
