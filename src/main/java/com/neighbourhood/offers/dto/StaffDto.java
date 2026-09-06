package com.neighbourhood.offers.dto;

import com.neighbourhood.offers.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDto {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Long shopId;
    private String shopName;
    private LocalDateTime createdAt;

    public static StaffDto fromEntity(User user) {
        return StaffDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .shopId(user.getShop() != null ? user.getShop().getId() : null)
                .shopName(user.getShop() != null ? user.getShop().getName() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
