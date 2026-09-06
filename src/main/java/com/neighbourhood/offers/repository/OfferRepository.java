package com.neighbourhood.offers.repository;

import com.neighbourhood.offers.entity.Offer;
import com.neighbourhood.offers.entity.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByShopIdOrderByCreatedAtDesc(Long shopId);

    @Query("SELECT o FROM Offer o WHERE o.status = :status AND (o.endDate IS NULL OR o.endDate > :now) ORDER BY o.createdAt DESC")
    List<Offer> findActiveUnexpiredOffers(@Param("status") OfferStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT o FROM Offer o WHERE o.shop.id = :shopId AND o.status = :status")
    List<Offer> findByShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") OfferStatus status);
}
