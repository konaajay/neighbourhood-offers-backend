package com.neighbourhood.offers.dto;

import com.neighbourhood.offers.entity.OfferStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOfferStatusRequest {
    @NotNull(message = "Status is required")
    private OfferStatus status;
}
