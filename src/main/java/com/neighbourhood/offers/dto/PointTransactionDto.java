package com.neighbourhood.offers.dto;

import com.neighbourhood.offers.entity.PointTransaction;
import com.neighbourhood.offers.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTransactionDto {
    private Long id;
    private Long shopId;
    private Long claimId;
    private TransactionType transactionType;
    private Integer pointsAmount;
    private Integer balanceAfter;
    private String description;
    private LocalDateTime createdAt;

    public static PointTransactionDto fromEntity(PointTransaction pt) {
        return PointTransactionDto.builder()
                .id(pt.getId())
                .shopId(pt.getShop().getId())
                .claimId(pt.getClaim() != null ? pt.getClaim().getId() : null)
                .transactionType(pt.getTransactionType())
                .pointsAmount(pt.getPointsAmount())
                .balanceAfter(pt.getBalanceAfter())
                .description(pt.getDescription())
                .createdAt(pt.getCreatedAt())
                .build();
    }
}
