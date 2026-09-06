package com.neighbourhood.offers.repository;

import com.neighbourhood.offers.entity.PointRequestStatus;
import com.neighbourhood.offers.entity.PointTopUpRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointTopUpRequestRepository extends JpaRepository<PointTopUpRequest, Long> {
    List<PointTopUpRequest> findByShopIdOrderByRequestedAtDesc(Long shopId);
    List<PointTopUpRequest> findByStatusOrderByRequestedAtDesc(PointRequestStatus status);
    List<PointTopUpRequest> findAllByOrderByRequestedAtDesc();
}
