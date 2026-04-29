package com.pythonadmin.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BannerRepository extends JpaRepository<BannerEntity, Integer> {
    Page<BannerEntity> findByEnabledTrueOrderBySortOrderAsc(Pageable pageable);
}
