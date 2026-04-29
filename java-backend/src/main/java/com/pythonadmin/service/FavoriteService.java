package com.pythonadmin.service;

import com.pythonadmin.domain.FavoriteEntity;
import com.pythonadmin.domain.FavoriteRepository;
import com.pythonadmin.domain.ProductEntity;
import com.pythonadmin.domain.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class FavoriteService {
    private final FavoriteRepository favoriteRepo;
    private final ProductRepository productRepo;

    public FavoriteService(FavoriteRepository favoriteRepo, ProductRepository productRepo) {
        this.favoriteRepo = favoriteRepo;
        this.productRepo = productRepo;
    }

    public Page<FavoriteEntity> listFavorites(Integer userId, Pageable pageable) {
        return favoriteRepo.findByUserId(userId, pageable);
    }

    @Transactional
    public FavoriteEntity addFavorite(Integer userId, Integer productId) {
        // 检查产品是否存在
        ProductEntity product = productRepo.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "产品不存在"));

        // 检查是否已收藏
        if (favoriteRepo.existsByUserIdAndProductId(userId, productId)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "已收藏过该产品");
        }

        FavoriteEntity favorite = new FavoriteEntity();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        return favoriteRepo.save(favorite);
    }

    @Transactional
    public void removeFavorite(Integer userId, Integer productId) {
        favoriteRepo.deleteByUserIdAndProductId(userId, productId);
    }

    public boolean isFavorited(Integer userId, Integer productId) {
        return favoriteRepo.existsByUserIdAndProductId(userId, productId);
    }
}
