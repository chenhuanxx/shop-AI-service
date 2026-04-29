package com.pythonadmin.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Integer> {
    
    Page<OrderEntity> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    
    Optional<OrderEntity> findByOrderNo(String orderNo);
    
    Optional<OrderEntity> findByIdAndUserId(Integer id, Integer userId);
}
