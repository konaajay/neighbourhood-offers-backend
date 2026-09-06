package com.neighbourhood.offers.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShopRequest {

    @NotBlank(message = "Shop name is required")
    private String name;

    private String category;

    private String address;

    private String locality;

    @Builder.Default
    private Integer initialPoints = 100;

    @Builder.Default
    private Integer costPerRedemption = 10;
}
