package com.neighbourhood.offers.dto;

import com.neighbourhood.offers.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoPersonaDto {
    private String email;
    private String fullName;
    private Role role;
    private Long shopId;
    private String shopName;
    private String description;
    private String tag;
    private String color;
}
