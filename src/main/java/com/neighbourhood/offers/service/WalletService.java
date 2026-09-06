package com.neighbourhood.offers.service;

import com.neighbourhood.offers.dto.*;
import com.neighbourhood.offers.entity.*;
import com.neighbourhood.offers.exception.ResourceNotFoundException;
import com.neighbourhood.offers.exception.UnauthorizedShopAccessException;
import com.neighbourhood.offers.repository.PointTopUpRequestRepository;
import com.neighbourhood.offers.repository.PointTransactionRepository;
import com.neighbourhood.offers.repository.ShopRepository;
import com.neighbourhood.offers.repository.UserRepository;
import com.neighbourhood.offers.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final ShopRepository shopRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PointTopUpRequestRepository pointTopUpRequestRepository;
    private final UserRepository userRepository;

    @Value("${app.business.low-points-threshold:20}")
    private int lowPointsThreshold;

    @Transactional(readOnly = true)
    public WalletDto getWallet(UserPrincipal currentUser) {
        if (currentUser.getShopId() == null) {
            throw new UnauthorizedShopAccessException("Current user does not manage a shop");
        }

        Shop shop = shopRepository.findById(currentUser.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        return WalletDto.builder()
                .shopId(shop.getId())
                .shopName(shop.getName())
                .pointsBalance(shop.getPointsBalance())
                .costPerRedemption(shop.getCostPerRedemption())
                .isLowBalance(shop.getPointsBalance() <= lowPointsThreshold)
                .lowBalanceThreshold(lowPointsThreshold)
                .build();
    }

    /**
     * Submit a point top-up request to the Super Admin.
     * Only the Super Admin has authority to credit points.
     */
    @Transactional
    public PointTopUpRequestResponseDto requestPoints(CreatePointRequestDto request, UserPrincipal currentUser) {
        if (currentUser.getShopId() == null) {
            throw new UnauthorizedShopAccessException("Current user does not manage a shop");
        }

        Shop shop = shopRepository.findById(currentUser.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        User shopkeeper = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Shopkeeper user not found"));

        PointTopUpRequest topUpRequest = PointTopUpRequest.builder()
                .shop(shop)
                .shopkeeper(shopkeeper)
                .pointsRequested(request.getPointsRequested())
                .reason(request.getReason())
                .status(PointRequestStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        PointTopUpRequest saved = pointTopUpRequestRepository.save(topUpRequest);
        log.info("Shopkeeper '{}' requested {} points for shop '{}'. Status: PENDING",
                shopkeeper.getEmail(), saved.getPointsRequested(), shop.getName());

        return PointTopUpRequestResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<PointTopUpRequestResponseDto> getMyPointRequests(UserPrincipal currentUser) {
        if (currentUser.getShopId() == null) {
            throw new UnauthorizedShopAccessException("Current user does not manage a shop");
        }

        return pointTopUpRequestRepository.findByShopIdOrderByRequestedAtDesc(currentUser.getShopId())
                .stream()
                .map(PointTopUpRequestResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Direct self top-up is restricted to ensure Super Admin platform control.
     */
    @Transactional
    public WalletDto topUp(TopUpRequest request, UserPrincipal currentUser) {
        throw new UnauthorizedShopAccessException(
                "Direct self top-up is disabled. Please submit a Top-Up Request to the Super Admin via the dashboard.");
    }

    @Transactional(readOnly = true)
    public List<PointTransactionDto> getTransactions(UserPrincipal currentUser) {
        if (currentUser.getShopId() == null) {
            throw new UnauthorizedShopAccessException("Current user does not manage a shop");
        }

        return pointTransactionRepository.findByShopIdOrderByCreatedAtDesc(currentUser.getShopId())
                .stream()
                .map(PointTransactionDto::fromEntity)
                .collect(Collectors.toList());
    }
}

