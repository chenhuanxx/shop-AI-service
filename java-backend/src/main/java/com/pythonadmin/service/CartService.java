package com.pythonadmin.service;

import com.pythonadmin.domain.CartEntity;
import com.pythonadmin.domain.CartRepository;
import com.pythonadmin.domain.ProductEntity;
import com.pythonadmin.domain.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CartService {
    private final CartRepository cartRepo;
    private final ProductRepository productRepo;

    public CartService(CartRepository cartRepo, ProductRepository productRepo) {
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
    }

    public List<CartEntity> getCartItems(Integer userId) {
        return cartRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public CartEntity addToCart(Integer userId, Integer productId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            quantity = 1;
        }

        // 检查产品是否存在且上架
        ProductEntity product = productRepo.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "产品不存在"));

        if (product.getStatus() != null && !product.getStatus()) {
            throw new ResponseStatusException(BAD_REQUEST, "产品已下架");
        }

        // 检查购物车中是否已有该产品
        Optional<CartEntity> existing = cartRepo.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            CartEntity item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            item.setUpdatedAt(Instant.now());
            return cartRepo.save(item);
        }

        // 新增购物车项
        CartEntity item = new CartEntity();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setPrice(product.getActualPrice() != null ? product.getActualPrice() : product.getOfficialPrice());
        return cartRepo.save(item);
    }

    @Transactional
    public CartEntity updateCartItem(Integer userId, Integer cartId, Integer quantity) {
        CartEntity item = cartRepo.findById(cartId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "购物车项不存在"));

        if (!item.getUserId().equals(userId)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "无权操作");
        }

        if (quantity == null || quantity < 1) {
            throw new ResponseStatusException(BAD_REQUEST, "数量必须大于0");
        }

        item.setQuantity(quantity);
        item.setUpdatedAt(Instant.now());
        return cartRepo.save(item);
    }

    @Transactional
    public void removeCartItem(Integer userId, Integer cartId) {
        CartEntity item = cartRepo.findById(cartId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "购物车项不存在"));

        if (!item.getUserId().equals(userId)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "无权操作");
        }

        cartRepo.delete(item);
    }

    @Transactional
    public void clearCart(Integer userId) {
        cartRepo.deleteByUserId(userId);
    }

    public int getCartCount(Integer userId) {
        return cartRepo.countByUserId(userId);
    }
}
