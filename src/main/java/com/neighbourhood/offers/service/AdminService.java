package com.neighbourhood.offers.service;

import com.neighbourhood.offers.dto.*;
import com.neighbourhood.offers.entity.*;
import com.neighbourhood.offers.exception.ResourceNotFoundException;
import com.neighbourhood.offers.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final OfferRepository offerRepository;
    private final ClaimRepository claimRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PointTopUpRequestRepository pointTopUpRequestRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminShopDto createShop(CreateShopRequest request) {
        Shop shop = Shop.builder()
                .name(request.getName())
                .category(request.getCategory())
                .address(request.getAddress())
                .locality(request.getLocality())
                .pointsBalance(request.getInitialPoints() != null ? request.getInitialPoints() : 100)
                .costPerRedemption(request.getCostPerRedemption() != null ? request.getCostPerRedemption() : 10)
                .createdAt(LocalDateTime.now())
                .build();

        Shop saved = shopRepository.save(shop);

        if (saved.getPointsBalance() > 0) {
            PointTransaction pt = PointTransaction.builder()
                    .shop(saved)
                    .transactionType(TransactionType.TOPUP)
                    .pointsAmount(saved.getPointsBalance())
                    .balanceAfter(saved.getPointsBalance())
                    .description("Initial points provisioned by Super Admin")
                    .createdAt(LocalDateTime.now())
                    .build();
            pointTransactionRepository.save(pt);
        }

        log.info("Super Admin created shop '{}' with {} initial points", saved.getName(), saved.getPointsBalance());

        return toAdminShopDto(saved);
    }

    @Transactional
    public User createShopkeeper(CreateShopkeeperRequest request) {
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with ID: " + request.getShopId()));

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }

        User shopkeeper = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(Role.ROLE_SHOPKEEPER)
                .shop(shop)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(shopkeeper);
        log.info("Super Admin created shopkeeper '{}' for shop '{}'", saved.getEmail(), shop.getName());
        return saved;
    }

    @Transactional
    public AdminShopDto topUpShopPoints(Long shopId, AdminTopUpRequest request) {
        Shop shop = shopRepository.findByIdForUpdate(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with ID: " + shopId));

        int newBalance = shop.getPointsBalance() + request.getPoints();
        shop.setPointsBalance(newBalance);
        Shop saved = shopRepository.save(shop);

        PointTransaction pt = PointTransaction.builder()
                .shop(saved)
                .transactionType(TransactionType.TOPUP)
                .pointsAmount(request.getPoints())
                .balanceAfter(newBalance)
                .description(request.getDescription() != null && !request.getDescription().isBlank()
                        ? request.getDescription()
                        : "Points added by Super Admin")
                .createdAt(LocalDateTime.now())
                .build();
        pointTransactionRepository.save(pt);

        log.info("Super Admin topped up shop '{}' (+{} points). New balance: {}", saved.getName(), request.getPoints(), newBalance);

        return toAdminShopDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AdminShopDto> getAllShops() {
        return shopRepository.findAll().stream()
                .map(this::toAdminShopDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<User> getAllShopkeepers() {
        return userRepository.findByRole(Role.ROLE_SHOPKEEPER);
    }

    @Transactional(readOnly = true)
    public PlatformStatsDto getPlatformStats() {
        long totalShops = shopRepository.count();
        long totalShopkeepers = userRepository.findByRole(Role.ROLE_SHOPKEEPER).size();
        long totalPoints = shopRepository.findAll().stream()
                .mapToLong(Shop::getPointsBalance)
                .sum();
        long totalRedemptions = claimRepository.findAll().stream()
                .filter(c -> c.getStatus() == ClaimStatus.REDEEMED)
                .count();
        long totalOffers = offerRepository.count();

        return PlatformStatsDto.builder()
                .totalShops(totalShops)
                .totalShopkeepers(totalShopkeepers)
                .totalPointsInCirculation(totalPoints)
                .totalRedemptions(totalRedemptions)
                .totalOffers(totalOffers)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PointTopUpRequestResponseDto> getPointRequests(PointRequestStatus status) {
        List<PointTopUpRequest> list;
        if (status != null) {
            list = pointTopUpRequestRepository.findByStatusOrderByRequestedAtDesc(status);
        } else {
            list = pointTopUpRequestRepository.findAllByOrderByRequestedAtDesc();
        }
        return list.stream()
                .map(PointTopUpRequestResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public PointTopUpRequestResponseDto approvePointRequest(Long requestId, ResolvePointRequestDto dto) {
        PointTopUpRequest req = pointTopUpRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Point request not found with ID: " + requestId));

        if (req.getStatus() != PointRequestStatus.PENDING) {
            throw new IllegalStateException("Cannot approve a request that is already " + req.getStatus());
        }

        Shop shop = shopRepository.findByIdForUpdate(req.getShop().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        int newBalance = shop.getPointsBalance() + req.getPointsRequested();
        shop.setPointsBalance(newBalance);
        shopRepository.save(shop);

        PointTransaction pt = PointTransaction.builder()
                .shop(shop)
                .transactionType(TransactionType.TOPUP)
                .pointsAmount(req.getPointsRequested())
                .balanceAfter(newBalance)
                .description("Top-up request #" + req.getId() + " approved by Super Admin" +
                        (dto != null && dto.getAdminNotes() != null && !dto.getAdminNotes().isBlank() ? " (" + dto.getAdminNotes() + ")" : ""))
                .createdAt(LocalDateTime.now())
                .build();
        pointTransactionRepository.save(pt);

        req.setStatus(PointRequestStatus.APPROVED);
        req.setResolvedAt(LocalDateTime.now());
        if (dto != null) {
            req.setAdminNotes(dto.getAdminNotes());
        }
        PointTopUpRequest saved = pointTopUpRequestRepository.save(req);

        log.info("Super Admin approved point request #{} for shop '{}' (+{} points). New balance: {}",
                saved.getId(), shop.getName(), saved.getPointsRequested(), newBalance);

        return PointTopUpRequestResponseDto.fromEntity(saved);
    }

    @Transactional
    public PointTopUpRequestResponseDto rejectPointRequest(Long requestId, ResolvePointRequestDto dto) {
        PointTopUpRequest req = pointTopUpRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Point request not found with ID: " + requestId));

        if (req.getStatus() != PointRequestStatus.PENDING) {
            throw new IllegalStateException("Cannot reject a request that is already " + req.getStatus());
        }

        req.setStatus(PointRequestStatus.REJECTED);
        req.setResolvedAt(LocalDateTime.now());
        if (dto != null) {
            req.setAdminNotes(dto.getAdminNotes());
        }
        PointTopUpRequest saved = pointTopUpRequestRepository.save(req);

        log.info("Super Admin rejected point request #{} for shop '{}'", saved.getId(), req.getShop().getName());

        return PointTopUpRequestResponseDto.fromEntity(saved);
    }

    private AdminShopDto toAdminShopDto(Shop shop) {
        User owner = userRepository.findFirstByShopIdAndRole(shop.getId(), Role.ROLE_SHOPKEEPER).orElse(null);
        List<Offer> offers = offerRepository.findByShopIdOrderByCreatedAtDesc(shop.getId());
        int activeOffers = (int) offers.stream().filter(Offer::isClaimable).count();
        int totalRedemptions = offers.stream().mapToInt(Offer::getRedemptionCount).sum();

        return AdminShopDto.builder()
                .id(shop.getId())
                .name(shop.getName())
                .category(shop.getCategory())
                .address(shop.getAddress())
                .locality(shop.getLocality())
                .pointsBalance(shop.getPointsBalance())
                .costPerRedemption(shop.getCostPerRedemption())
                .shopkeeperName(owner != null ? owner.getFullName() : "Unassigned")
                .shopkeeperEmail(owner != null ? owner.getEmail() : null)
                .shopkeeperId(owner != null ? owner.getId() : null)
                .activeOffersCount(activeOffers)
                .totalRedemptions(totalRedemptions)
                .createdAt(shop.getCreatedAt())
                .build();
    }
}
