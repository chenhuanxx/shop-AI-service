package com.pythonadmin.api;

import com.pythonadmin.domain.CartEntity;
import com.pythonadmin.domain.UserEntity;
import com.pythonadmin.service.CartService;
import com.pythonadmin.service.CurrentUserService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CartController {
    private final CartService cartService;
    private final CurrentUserService currentUserService;

    public CartController(CartService cartService, CurrentUserService currentUserService) {
        this.cartService = cartService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/cart")
    @Transactional(readOnly = true)
    public CartResponse getCart() {
        UserEntity user = currentUserService.requireUser();
        List<CartEntity> items = cartService.getCartItems(user.getId());
        List<CartItemResponse> itemResponses = items.stream()
                .map(this::toItemResponse)
                .toList();
        return new CartResponse(itemResponses);
    }

    @PostMapping("/cart/add")
    @Transactional
    public CartItemResponse addToCart(@RequestBody AddToCartRequest request) {
        UserEntity user = currentUserService.requireUser();
        CartEntity item = cartService.addToCart(user.getId(), request.productId(), request.quantity());
        return toItemResponse(item);
    }

    @PutMapping("/cart/update")
    @Transactional
    public CartItemResponse updateCartItem(@RequestBody UpdateCartRequest request) {
        UserEntity user = currentUserService.requireUser();
        CartEntity item = cartService.updateCartItem(user.getId(), request.id(), request.quantity());
        return toItemResponse(item);
    }

    @DeleteMapping("/cart/{id}")
    @Transactional
    public void removeCartItem(@PathVariable("id") Integer id) {
        UserEntity user = currentUserService.requireUser();
        cartService.removeCartItem(user.getId(), id);
    }

    @DeleteMapping("/cart/clear")
    @Transactional
    public void clearCart() {
        UserEntity user = currentUserService.requireUser();
        cartService.clearCart(user.getId());
    }

    private CartItemResponse toItemResponse(CartEntity item) {
        if (item == null) return null;
        return new CartItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProduct() != null ? item.getProduct().getName() : "",
                item.getProduct() != null ? item.getProduct().getThumbnail() : "",
                item.getPrice(),
                item.getQuantity()
        );
    }

    public record AddToCartRequest(Integer productId, Integer quantity) {}

    public record UpdateCartRequest(Integer id, Integer quantity) {}

    public record CartResponse(List<CartItemResponse> items) {}

    public record CartItemResponse(
            Integer id,
            Integer productId,
            String name,
            String thumbnail,
            java.math.BigDecimal price,
            Integer quantity
    ) {}
}
