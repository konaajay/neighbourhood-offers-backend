package com.neighbourhood.offers.repository;

import com.neighbourhood.offers.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    java.util.List<User> findByRole(com.neighbourhood.offers.entity.Role role);
    Optional<User> findFirstByShopIdAndRole(Long shopId, com.neighbourhood.offers.entity.Role role);
    java.util.List<User> findByShopIdAndRole(Long shopId, com.neighbourhood.offers.entity.Role role);
    java.util.List<User> findByShopId(Long shopId);
}
