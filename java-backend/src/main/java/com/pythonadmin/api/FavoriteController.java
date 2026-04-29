package com.pythonadmin.api;

import com.pythonadmin.domain.FavoriteEntity;
import com.pythonadmin.domain.UserEntity;
import com.pythonadmin.service.CurrentUserService;
import com.pythonadmin.service.FavoriteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
public class FavoriteController {
    private final FavoriteService favoriteService;
    private final CurrentUserService currentUserService;

    public FavoriteController(FavoriteService favoriteService, CurrentUserService currentUserService) {
        this.favoriteService = favoriteService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/favorites")
    @Transactional(readOnly = true)
    public Page<FavoriteResponse> listFavorites(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int pageSize
    ) {
        UserEntity user = currentUserService.requireUser();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, pageSize));
        Page<FavoriteEntity> entities = favoriteService.listFavorites(user.getId(), PageRequest.of(safePage - 1, safeSize));
        return entities.map(this::toResponse);
    }

    @PostMapping("/favorites/add")
    @Transactional
    public FavoriteResponse addFavorite(@RequestBody AddFavoriteRequest request) {
        UserEntity user = currentUserService.requireUser();
        FavoriteEntity favorite = favoriteService.addFavorite(user.getId(), request.productId());
        return toResponse(favorite);
    }

    @DeleteMapping("/favorites/{productId}")
    @Transactional
    public void removeFavorite(@PathVariable("productId") Integer productId) {
        UserEntity user = currentUserService.requireUser();
        favoriteService.removeFavorite(user.getId(), productId);
    }

    @GetMapping("/favorites/check/{productId}")
    @Transactional(readOnly = true)
    public boolean isFavorited(@PathVariable("productId") Integer productId) {
        UserEntity user = currentUserService.requireUser();
        return favoriteService.isFavorited(user.getId(), productId);
    }

    private FavoriteResponse toResponse(FavoriteEntity f) {
        if (f == null) return null;
        return new FavoriteResponse(
                f.getId(),
                f.getProductId(),
                f.getProduct() != null ? f.getProduct().getName() : "",
                f.getProduct() != null ? f.getProduct().getThumbnail() : "",
                f.getProduct() != null && f.getProduct().getActualPrice() != null 
                    ? f.getProduct().getActualPrice() 
                    : (f.getProduct() != null ? f.getProduct().getOfficialPrice() : null),
                f.getCreatedAt() != null ? f.getCreatedAt().toString() : ""
        );
    }

    public record AddFavoriteRequest(Integer productId) {}

    public record FavoriteResponse(
            Integer id,
            Integer productId,
            String productName,
            String productThumbnail,
            java.math.BigDecimal price,
            String createdAt
    ) {}
}
