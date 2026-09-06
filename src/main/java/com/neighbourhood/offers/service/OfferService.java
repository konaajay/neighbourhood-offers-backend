package com.neighbourhood.offers.service;

import com.neighbourhood.offers.dto.CreateOfferRequest;
import com.neighbourhood.offers.dto.OfferResponseDto;
import com.neighbourhood.offers.entity.Offer;
import com.neighbourhood.offers.entity.OfferStatus;
import com.neighbourhood.offers.entity.Shop;
import com.neighbourhood.offers.exception.ResourceNotFoundException;
import com.neighbourhood.offers.exception.UnauthorizedShopAccessException;
import com.neighbourhood.offers.repository.OfferRepository;
import com.neighbourhood.offers.repository.ShopRepository;
import com.neighbourhood.offers.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final ShopRepository shopRepository;

    @Transactional(readOnly = true)
    public List<OfferResponseDto> getOffersForShopkeeper(UserPrincipal currentUser) {
        if (currentUser.getShopId() == null) {
            throw new UnauthorizedShopAccessException("Current user is not associated with any shop");
        }
        return offerRepository.findByShopIdOrderByCreatedAtDesc(currentUser.getShopId())
                .stream()
                .map(OfferResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OfferResponseDto> getActiveOffersForShoppers(String category, String search) {
        LocalDateTime now = LocalDateTime.now();
        List<Offer> offers = offerRepository.findActiveUnexpiredOffers(OfferStatus.ACTIVE, now);

        return offers.stream()
                .filter(o -> category == null || category.isBlank() || category.equalsIgnoreCase("ALL") || 
                        (o.getApplicableCategory() != null && o.getApplicableCategory().toLowerCase().contains(category.toLowerCase())) ||
                        (o.getShop().getCategory() != null && o.getShop().getCategory().toLowerCase().contains(category.toLowerCase())))
                .filter(o -> search == null || search.isBlank() ||
                        o.getTitle().toLowerCase().contains(search.toLowerCase()) ||
                        o.getShop().getName().toLowerCase().contains(search.toLowerCase()) ||
                        (o.getDescription() != null && o.getDescription().toLowerCase().contains(search.toLowerCase())))
                .map(OfferResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OfferResponseDto getOfferById(Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + id));
        return OfferResponseDto.fromEntity(offer);
    }

    @Transactional
    public OfferResponseDto createOffer(CreateOfferRequest request, UserPrincipal currentUser) {
        if (currentUser.getShopId() == null) {
            throw new UnauthorizedShopAccessException("User has no associated shop to create offers");
        }

        Shop shop = shopRepository.findById(currentUser.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + currentUser.getShopId()));

        Offer offer = Offer.builder()
                .shop(shop)
                .title(request.getTitle())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minBillAmount(request.getMinBillAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .applicableCategory(request.getApplicableCategory())
                .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now())
                .endDate(request.getEndDate() != null ? request.getEndDate() : LocalDateTime.now().plusDays(30))
                .claimLimit(request.getClaimLimit() != null ? request.getClaimLimit() : 100)
                .status(request.getStatus() != null ? request.getStatus() : OfferStatus.ACTIVE)
                .rawAiPrompt(request.getRawAiPrompt())
                .claimsCount(0)
                .redemptionCount(0)
                .build();

        Offer savedOffer = offerRepository.save(offer);
        return OfferResponseDto.fromEntity(savedOffer);
    }

    @Transactional
    public OfferResponseDto updateOffer(Long offerId, CreateOfferRequest request, UserPrincipal currentUser) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + offerId));

        validateShopOwnership(offer, currentUser, "modify");

        offer.setTitle(request.getTitle());
        offer.setDescription(request.getDescription());
        offer.setDiscountType(request.getDiscountType());
        offer.setDiscountValue(request.getDiscountValue());
        offer.setMinBillAmount(request.getMinBillAmount());
        offer.setMaxDiscountAmount(request.getMaxDiscountAmount());
        offer.setApplicableCategory(request.getApplicableCategory());
        if (request.getStartDate() != null) offer.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) offer.setEndDate(request.getEndDate());
        if (request.getClaimLimit() != null) offer.setClaimLimit(request.getClaimLimit());
        if (request.getStatus() != null) offer.setStatus(request.getStatus());

        return OfferResponseDto.fromEntity(offerRepository.save(offer));
    }

    @Transactional
    public OfferResponseDto updateOfferStatus(Long offerId, OfferStatus status, UserPrincipal currentUser) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + offerId));

        validateShopOwnership(offer, currentUser, "switch status of");

        offer.setStatus(status);
        return OfferResponseDto.fromEntity(offerRepository.save(offer));
    }

    @Transactional
    public void deleteOffer(Long offerId, UserPrincipal currentUser) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + offerId));

        validateShopOwnership(offer, currentUser, "delete");
        offer.setStatus(OfferStatus.INACTIVE);
        offerRepository.save(offer);
    }

    private void validateShopOwnership(Offer offer, UserPrincipal currentUser, String action) {
        if (currentUser.getShopId() == null || !offer.getShop().getId().equals(currentUser.getShopId())) {
            throw new UnauthorizedShopAccessException(
                    String.format("Multi-tenant violation: You do not own shop '%s' (ID: %d) and cannot %s this offer.",
                            offer.getShop().getName(), offer.getShop().getId(), action)
            );
        }
    }
}
