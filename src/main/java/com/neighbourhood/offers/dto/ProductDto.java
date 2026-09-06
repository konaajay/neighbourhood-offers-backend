package com.neighbourhood.offers.dto;

import com.neighbourhood.offers.entity.Product;
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
public class ProductDto {
    private Long id;
    private Long offerId;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private LocalDateTime createdAt;

    public static ProductDto fromEntity(Product p) {
        return ProductDto.builder()
                .id(p.getId())
                .offerId(p.getOffer() != null ? p.getOffer().getId() : null)
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
