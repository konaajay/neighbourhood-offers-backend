package com.neighbourhood.offers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiParseResponseDto {
    private String title;
    private String description;
    private String discountType; // PERCENTAGE, FLAT_AMOUNT, BOGO
    private BigDecimal discountValue;
    private BigDecimal minBillAmount;
    private BigDecimal maxDiscountAmount;
    private String applicableCategory;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<String> termsAndConditions;
    private String rawInput;
    private String confidenceNotes;
}
