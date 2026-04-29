package com.pythonadmin.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Integer> {
    
    Page<FavoriteEntity> findByUserId(Integer userId, Pageable pageable);
    
    List<FavoriteEntity> findByUserId(Integer userId);
    
    Optional<FavoriteEntity> findByUserIdAndProductId(Integer userId, Integer productId);
    
    boolean existsByUserIdAndProductId(Integer userId, Integer productId);
    
    void deleteByUserIdAndProductId(Integer userId, Integer productId);
    
    @Query("SELECT f.productId FROM FavoriteEntity f WHERE f.userId = :userId")
    List<Integer> findProductIdsByUserId(@Param("userId") Integer userId);
}
