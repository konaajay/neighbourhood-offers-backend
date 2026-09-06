package com.neighbourhood.offers.dto;

import com.neighbourhood.offers.entity.PointRequestStatus;
import com.neighbourhood.offers.entity.PointTopUpRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTopUpRequestResponseDto {
    private Long id;
    private Long shopId;
    private String shopName;
    private Long shopkeeperId;
    private String shopkeeperName;
    private String shopkeeperEmail;
    private int pointsRequested;
    private String reason;
    private PointRequestStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime resolvedAt;
    private String adminNotes;

    public static PointTopUpRequestResponseDto fromEntity(PointTopUpRequest req) {
        return PointTopUpRequestResponseDto.builder()
                .id(req.getId())
                .shopId(req.getShop() != null ? req.getShop().getId() : null)
                .shopName(req.getShop() != null ? req.getShop().getName() : null)
                .shopkeeperId(req.getShopkeeper() != null ? req.getShopkeeper().getId() : null)
                .shopkeeperName(req.getShopkeeper() != null ? req.getShopkeeper().getFullName() : null)
                .shopkeeperEmail(req.getShopkeeper() != null ? req.getShopkeeper().getEmail() : null)
                .pointsRequested(req.getPointsRequested())
                .reason(req.getReason())
                .status(req.getStatus())
                .requestedAt(req.getRequestedAt())
                .resolvedAt(req.getResolvedAt())
                .adminNotes(req.getAdminNotes())
                .build();
    }
}
