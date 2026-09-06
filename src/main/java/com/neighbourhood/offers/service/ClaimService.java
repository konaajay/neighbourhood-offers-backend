package com.neighbourhood.offers.service;

import com.neighbourhood.offers.dto.ClaimResponseDto;
import com.neighbourhood.offers.dto.CreateClaimRequest;
import com.neighbourhood.offers.entity.*;
import com.neighbourhood.offers.exception.OfferExpiredException;
import com.neighbourhood.offers.exception.OfferNotClaimableException;
import com.neighbourhood.offers.exception.ResourceNotFoundException;
import com.neighbourhood.offers.repository.ClaimRepository;
import com.neighbourhood.offers.repository.OfferRepository;
import com.neighbourhood.offers.repository.UserRepository;
import com.neighbourhood.offers.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final OfferRepository offerRepository;
    private final UserRepository userRepository;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUM = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    @Transactional
    public ClaimResponseDto claimOffer(Long offerId, UserPrincipal currentUser) {
        return claimOffer(offerId, null, currentUser);
    }

    @Transactional
    public ClaimResponseDto claimOffer(Long offerId, CreateClaimRequest request, UserPrincipal currentUser) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + offerId));

        if (!offer.isClaimable()) {
            if (offer.isExpired()) {
                throw new OfferExpiredException("This offer has expired and can no longer be claimed.");
            }
            throw new OfferNotClaimableException("This offer is currently inactive or not claimable.");
        }

        User shopper = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Shopper account not found"));

        // Generate unique claim code
        String claimCode = generateUniqueClaimCode();

        LocalDateTime claimedAt = LocalDateTime.now();
        LocalDateTime expiresAt = offer.getEndDate() != null && offer.getEndDate().isBefore(claimedAt.plusDays(3))
                ? offer.getEndDate()
                : claimedAt.plusDays(3);

        Claim claim = Claim.builder()
                .claimCode(claimCode)
                .offer(offer)
                .shop(offer.getShop())
                .shopper(shopper)
                .status(ClaimStatus.CLAIMED)
                .claimedAt(claimedAt)
                .expiresAt(expiresAt)
                .cartSnapshotJson(request != null ? request.getCartSnapshotJson() : null)
                .estimatedTotal(request != null ? request.getEstimatedTotal() : null)
                .build();

        // Increment offer claims count
        offer.setClaimsCount(offer.getClaimsCount() + 1);
        offerRepository.save(offer);

        Claim savedClaim = claimRepository.save(claim);

        // NOTE: Pay for footfall promise: No points are deducted on claim!
        return ClaimResponseDto.fromEntity(savedClaim);
    }

    @Transactional(readOnly = true)
    public List<ClaimResponseDto> getShopperClaims(UserPrincipal currentUser) {
        return claimRepository.findByShopperIdOrderByClaimedAtDesc(currentUser.getId())
                .stream()
                .map(ClaimResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    private String generateUniqueClaimCode() {
        while (true) {
            StringBuilder sb = new StringBuilder("NBR-");
            for (int i = 0; i < 4; i++) {
                sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
            }
            sb.append("-");
            for (int i = 0; i < 2; i++) {
                sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
            }
            String candidate = sb.toString();
            if (claimRepository.findByClaimCode(candidate).isEmpty()) {
                return candidate;
            }
        }
    }
}
