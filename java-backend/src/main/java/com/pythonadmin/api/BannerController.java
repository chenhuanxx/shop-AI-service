package com.pythonadmin.api;

import com.pythonadmin.domain.BannerEntity;
import com.pythonadmin.service.BannerService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/banners")
public class BannerController {
    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping
    public Page<BannerEntity> listBanners(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return bannerService.listBanners(page, size);
    }

    @GetMapping("/enabled")
    public Page<BannerEntity> listEnabledBanners(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return bannerService.listEnabledBanners(page, size);
    }

    @GetMapping("/{id}")
    public BannerEntity getBanner(@PathVariable Integer id) {
        return bannerService.getBannerById(id);
    }

    @PostMapping
    public BannerEntity createBanner(@RequestBody BannerEntity banner) {
        return bannerService.createBanner(banner);
    }

    @PutMapping("/{id}")
    public BannerEntity updateBanner(@PathVariable Integer id, @RequestBody BannerEntity banner) {
        return bannerService.updateBanner(id, banner);
    }

    @DeleteMapping("/{id}")
    public void deleteBanner(@PathVariable Integer id) {
        bannerService.deleteBanner(id);
    }
}
