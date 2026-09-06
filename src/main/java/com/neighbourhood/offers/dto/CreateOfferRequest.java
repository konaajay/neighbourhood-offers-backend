package com.neighbourhood.offers.dto;

import com.neighbourhood.offers.entity.OfferStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOfferRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Discount type is required")
    private String discountType; // PERCENTAGE, FLAT_AMOUNT, BOGO

    @NotNull(message = "Discount value is required")
    @Positive(message = "Discount value must be positive")
    private BigDecimal discountValue;

    private BigDecimal minBillAmount;

    private BigDecimal maxDiscountAmount;

    private String applicableCategory;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer claimLimit;

    private OfferStatus status;

    private String rawAiPrompt;
}
