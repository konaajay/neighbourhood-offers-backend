package com.neighbourhood.offers.repository;

import com.neighbourhood.offers.entity.Claim;
import com.neighbourhood.offers.entity.ClaimStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByClaimCode(String claimCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Claim c WHERE c.claimCode = :claimCode")
    Optional<Claim> findByClaimCodeForUpdate(@Param("claimCode") String claimCode);

    List<Claim> findByShopperIdOrderByClaimedAtDesc(Long shopperId);

    List<Claim> findByShopIdOrderByClaimedAtDesc(Long shopId);

    @Query("SELECT c FROM Claim c WHERE c.shop.id = :shopId AND c.status = 'REDEEMED' AND c.redeemedAt >= :startDate AND c.redeemedAt <= :endDate ORDER BY c.redeemedAt ASC")
    List<Claim> findRedeemedClaimsInPeriod(@Param("shopId") Long shopId, 
                                          @Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);

    long countByOfferIdAndShopperIdAndStatus(Long offerId, Long shopperId, ClaimStatus status);
}
