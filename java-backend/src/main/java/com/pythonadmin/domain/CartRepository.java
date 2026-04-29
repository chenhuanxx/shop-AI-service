package com.pythonadmin.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<CartEntity, Integer> {
    
    List<CartEntity> findByUserIdOrderByCreatedAtDesc(Integer userId);
    
    Optional<CartEntity> findByUserIdAndProductId(Integer userId, Integer productId);
    
    void deleteByUserId(Integer userId);
    
    void deleteByUserIdAndProductId(Integer userId, Integer productId);
    
    int countByUserId(Integer userId);
}
