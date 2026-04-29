package com.pythonadmin.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, Integer> {
    Page<ProductEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<ProductEntity> findByStatusTrue(Pageable pageable);

    @Query("SELECT p FROM ProductEntity p WHERE (:status IS NULL OR p.status = :status) AND (:categoryId IS NULL OR p.category.id = :categoryId)")
    Page<ProductEntity> findByFilters(@Param("status") Boolean status, @Param("categoryId") Integer categoryId, Pageable pageable);

    @Query("SELECT p FROM ProductEntity p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND (:status IS NULL OR p.status = :status) AND (:categoryId IS NULL OR p.category.id = :categoryId)")
    Page<ProductEntity> findByNameContainingIgnoreCaseAndFilters(@Param("name") String name, @Param("status") Boolean status, @Param("categoryId") Integer categoryId, Pageable pageable);
}
