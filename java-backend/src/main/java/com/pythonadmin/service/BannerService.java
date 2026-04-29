package com.pythonadmin.service;

import com.pythonadmin.domain.BannerEntity;
import com.pythonadmin.domain.BannerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class BannerService {
    private final BannerRepository bannerRepo;

    public BannerService(BannerRepository bannerRepo) {
        this.bannerRepo = bannerRepo;
    }

    public Page<BannerEntity> listBanners(int page, int size) {
        return bannerRepo.findAll(PageRequest.of(page - 1, size));
    }

    public Page<BannerEntity> listEnabledBanners(int page, int size) {
        return bannerRepo.findByEnabledTrueOrderBySortOrderAsc(PageRequest.of(page - 1, size));
    }

    public BannerEntity getBannerById(Integer id) {
        return bannerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner不存在"));
    }

    @Transactional
    public BannerEntity createBanner(BannerEntity banner) {
        return bannerRepo.save(banner);
    }

    @Transactional
    public BannerEntity updateBanner(Integer id, BannerEntity updates) {
        BannerEntity existing = getBannerById(id);
        if (updates.getTitle() != null) existing.setTitle(updates.getTitle());
        if (updates.getImageUrl() != null) existing.setImageUrl(updates.getImageUrl());
        if (updates.getLinkUrl() != null) existing.setLinkUrl(updates.getLinkUrl());
        if (updates.getSortOrder() != null) existing.setSortOrder(updates.getSortOrder());
        if (updates.getEnabled() != null) existing.setEnabled(updates.getEnabled());
        return bannerRepo.save(existing);
    }

    @Transactional
    public void deleteBanner(Integer id) {
        if (!bannerRepo.existsById(id)) {
            throw new RuntimeException("Banner不存在");
        }
        bannerRepo.deleteById(id);
    }
}
