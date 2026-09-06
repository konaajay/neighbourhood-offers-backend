package com.neighbourhood.offers.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String category;

    private String address;

    private String locality;

    @Column(nullable = false)
    @Builder.Default
    private Integer pointsBalance = 100;

    @Column(nullable = false)
    @Builder.Default
    private Integer costPerRedemption = 10;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (pointsBalance == null) {
            pointsBalance = 100;
        }
        if (costPerRedemption == null) {
            costPerRedemption = 10;
        }
    }
}
