package com.neighbourhood.offers.controller;

import com.neighbourhood.offers.dto.CreateProductRequest;
import com.neighbourhood.offers.dto.ProductDto;
import com.neighbourhood.offers.security.UserPrincipal;
import com.neighbourhood.offers.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping({"/shopper/offers/{offerId}/products", "/counter/offers/{offerId}/products"})
    public ResponseEntity<List<ProductDto>> getProductsByOffer(@PathVariable Long offerId) {
        return ResponseEntity.ok(productService.getProductsByOffer(offerId));
    }

    @GetMapping({"/shopper/shops/{shopId}/products", "/counter/shops/{shopId}/products"})
    public ResponseEntity<List<ProductDto>> getProductsByShop(@PathVariable Long shopId) {
        return ResponseEntity.ok(productService.getProductsByShop(shopId));
    }

    @PostMapping("/shopkeeper/offers/{offerId}/products")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public ResponseEntity<ProductDto> addProduct(
            @PathVariable Long offerId,
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.addProductToOffer(offerId, request, currentUser));
    }

    @PutMapping("/shopkeeper/products/{id}")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(productService.updateProduct(id, request, currentUser));
    }

    @DeleteMapping("/shopkeeper/products/{id}")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        productService.deleteProduct(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
