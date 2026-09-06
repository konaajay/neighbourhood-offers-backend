package com.neighbourhood.offers.service;

import com.neighbourhood.offers.dto.CreateProductRequest;
import com.neighbourhood.offers.dto.ProductDto;
import com.neighbourhood.offers.entity.Offer;
import com.neighbourhood.offers.entity.Product;
import com.neighbourhood.offers.entity.Role;
import com.neighbourhood.offers.exception.ResourceNotFoundException;
import com.neighbourhood.offers.exception.UnauthorizedShopAccessException;
import com.neighbourhood.offers.repository.OfferRepository;
import com.neighbourhood.offers.repository.ProductRepository;
import com.neighbourhood.offers.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final OfferRepository offerRepository;

    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByOffer(Long offerId) {
        if (!offerRepository.existsById(offerId)) {
            throw new ResourceNotFoundException("Offer not found with id: " + offerId);
        }
        return productRepository.findByOfferIdOrderByIdAsc(offerId)
                .stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByShop(Long shopId) {
        return productRepository.findByOffer_Shop_IdOrderByIdAsc(shopId)
                .stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDto addProductToOffer(Long offerId, CreateProductRequest request, UserPrincipal currentUser) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + offerId));

        validateOwnership(offer, currentUser, "add products to");

        Product product = Product.builder()
                .offer(offer)
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .build();

        Product saved = productRepository.save(product);
        return ProductDto.fromEntity(saved);
    }

    @Transactional
    public ProductDto updateProduct(Long productId, CreateProductRequest request, UserPrincipal currentUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        validateOwnership(product.getOffer(), currentUser, "modify product in");

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }

        return ProductDto.fromEntity(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long productId, UserPrincipal currentUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        validateOwnership(product.getOffer(), currentUser, "delete product from");

        productRepository.delete(product);
    }

    private void validateOwnership(Offer offer, UserPrincipal currentUser, String action) {
        if (currentUser.getRole() != Role.ROLE_SHOPKEEPER) {
            throw new UnauthorizedShopAccessException(
                    "Counter staff and non-shopkeeper roles have read-only access to products. Only shopkeepers can modify products."
            );
        }
        if (currentUser.getShopId() == null || !offer.getShop().getId().equals(currentUser.getShopId())) {
            throw new UnauthorizedShopAccessException(
                    String.format("Multi-tenant violation: You do not own shop '%s' (ID: %d) and cannot %s this offer.",
                            offer.getShop().getName(), offer.getShop().getId(), action)
            );
        }
    }
}
