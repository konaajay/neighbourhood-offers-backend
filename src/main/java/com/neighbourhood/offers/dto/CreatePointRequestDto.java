package com.neighbourhood.offers.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePointRequestDto {
    @NotNull(message = "Points requested is required")
    @Min(value = 10, message = "Minimum requested points is 10")
    private Integer pointsRequested;

    private String reason;
}
