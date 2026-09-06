package com.neighbourhood.offers.repository;

import com.neighbourhood.offers.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByOfferIdOrderByIdAsc(Long offerId);
    List<Product> findByOffer_Shop_IdOrderByIdAsc(Long shopId);
}
