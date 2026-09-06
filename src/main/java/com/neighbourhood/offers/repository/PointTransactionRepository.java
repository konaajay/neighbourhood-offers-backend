package com.neighbourhood.offers.repository;

import com.neighbourhood.offers.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    List<PointTransaction> findByShopIdOrderByCreatedAtDesc(Long shopId);

    @Query("SELECT pt FROM PointTransaction pt WHERE pt.shop.id = :shopId AND pt.createdAt >= :startDate AND pt.createdAt <= :endDate ORDER BY pt.createdAt DESC")
    List<PointTransaction> findByShopIdAndPeriod(@Param("shopId") Long shopId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);
}
