package com.neighbourhood.offers.dto;

import com.neighbourhood.offers.entity.Offer;
import com.neighbourhood.offers.entity.OfferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferResponseDto {
    private Long id;
    private Long shopId;
    private String shopName;
    private String shopCategory;
    private String shopAddress;
    private String shopLocality;
    private Integer shopPointsBalance;

    private String title;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minBillAmount;
    private BigDecimal maxDiscountAmount;
    private String applicableCategory;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer claimLimit;
    private Integer claimsCount;
    private Integer redemptionCount;
    private OfferStatus status;
    private Boolean isExpired;
    private Boolean isClaimable;
    private String rawAiPrompt;
    private LocalDateTime createdAt;

    private List<ProductDto> products;
    private String primaryImageUrl;
    private BigDecimal startingPrice;

    public static OfferResponseDto fromEntity(Offer offer) {
        List<ProductDto> productDtos = offer.getProducts() != null
                ? offer.getProducts().stream().map(ProductDto::fromEntity).collect(Collectors.toList())
                : Collections.emptyList();

        String primaryImg = null;
        BigDecimal minPrice = null;
        if (!productDtos.isEmpty()) {
            primaryImg = productDtos.stream()
                    .map(ProductDto::getImageUrl)
                    .filter(img -> img != null && !img.isBlank())
                    .findFirst()
                    .orElse(null);

            minPrice = productDtos.stream()
                    .map(ProductDto::getPrice)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(null);
        }

        return OfferResponseDto.builder()
                .id(offer.getId())
                .shopId(offer.getShop().getId())
                .shopName(offer.getShop().getName())
                .shopCategory(offer.getShop().getCategory())
                .shopAddress(offer.getShop().getAddress())
                .shopLocality(offer.getShop().getLocality())
                .shopPointsBalance(offer.getShop().getPointsBalance())
                .title(offer.getTitle())
                .description(offer.getDescription())
                .discountType(offer.getDiscountType())
                .discountValue(offer.getDiscountValue())
                .minBillAmount(offer.getMinBillAmount())
                .maxDiscountAmount(offer.getMaxDiscountAmount())
                .applicableCategory(offer.getApplicableCategory())
                .startDate(offer.getStartDate())
                .endDate(offer.getEndDate())
                .claimLimit(offer.getClaimLimit())
                .claimsCount(offer.getClaimsCount())
                .redemptionCount(offer.getRedemptionCount())
                .status(offer.getStatus())
                .isExpired(offer.isExpired())
                .isClaimable(offer.isClaimable())
                .rawAiPrompt(offer.getRawAiPrompt())
                .createdAt(offer.getCreatedAt())
                .products(productDtos)
                .primaryImageUrl(primaryImg)
                .startingPrice(minPrice)
                .build();
    }
}
