package com.neighbourhood.offers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyFootfallDto {
    private LocalDate date;
    private String dayOfWeek;
    private Integer redemptionsCount;
    private Integer pointsSpent;
}
