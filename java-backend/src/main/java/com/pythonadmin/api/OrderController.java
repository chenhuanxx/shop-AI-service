package com.pythonadmin.api;

import com.pythonadmin.domain.OrderEntity;
import com.pythonadmin.domain.OrderItemEntity;
import com.pythonadmin.domain.UserEntity;
import com.pythonadmin.service.AdminService;
import com.pythonadmin.service.CurrentUserService;
import com.pythonadmin.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class OrderController {
    private final OrderService orderService;
    private final CurrentUserService currentUserService;
    private final AdminService adminService;

    public OrderController(OrderService orderService, CurrentUserService currentUserService, AdminService adminService) {
        this.orderService = orderService;
        this.currentUserService = currentUserService;
        this.adminService = adminService;
    }

    // 用户订单接口
    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int pageSize
    ) {
        UserEntity user = currentUserService.requireUser();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, pageSize));
        Page<OrderEntity> orders = orderService.listOrders(user.getId(), PageRequest.of(safePage - 1, safeSize));
        return orders.map(this::toOrderResponse);
    }

    @GetMapping("/orders/{id}")
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(@PathVariable("id") Integer id) {
        UserEntity user = currentUserService.requireUser();
        OrderEntity order = orderService.getOrderDetail(user.getId(), id);
        return toOrderDetailResponse(order);
    }

    @PostMapping("/orders/create")
    @Transactional
    public OrderResponse createOrder(@RequestBody OrderService.CreateOrderRequest request) {
        UserEntity user = currentUserService.requireUser();
        OrderEntity order = orderService.createOrder(user.getId(), request);
        return toOrderResponse(order);
    }

    @PostMapping("/orders/{id}/pay")
    @Transactional
    public OrderResponse confirmPay(@PathVariable("id") Integer id) {
        UserEntity user = currentUserService.requireUser();
        OrderEntity order = orderService.confirmPay(user.getId(), id);
        return toOrderResponse(order);
    }

    // 管理员订单接口
    @GetMapping("/admin/orders")
    @Transactional(readOnly = true)
    public Page<AdminOrderResponse> listAllOrders(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status
    ) {
        checkAdmin();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, pageSize));
        Page<OrderEntity> orders = orderService.listAllOrders(keyword, status, PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return orders.map(this::toAdminOrderResponse);
    }

    @GetMapping("/admin/orders/{id}")
    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getAdminOrderDetail(@PathVariable("id") Integer id) {
        checkAdmin();
        OrderEntity order = orderService.getOrderById(id);
        return toAdminOrderDetailResponse(order);
    }

    @PutMapping("/admin/orders/{id}/status")
    @Transactional
    public AdminOrderResponse updateOrderStatus(@PathVariable("id") Integer id, @RequestBody UpdateStatusRequest request) {
        checkAdmin();
        OrderEntity order = orderService.updateOrderStatus(id, request.status());
        return toAdminOrderResponse(order);
    }

    private void checkAdmin() {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
    }

    private OrderResponse toOrderResponse(OrderEntity order) {
        if (order == null) return null;
        List<OrderItemResponse> items = null;
        if (order.getItems() != null) {
            items = order.getItems().stream()
                    .map(this::toItemResponse)
                    .toList();
        }
        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getTotalAmount(),
                order.getStatus(),
                items,
                order.getCreatedAt() != null ? order.getCreatedAt().toString() : ""
        );
    }

    private OrderDetailResponse toOrderDetailResponse(OrderEntity order) {
        if (order == null) return null;
        List<OrderItemResponse> items = null;
        if (order.getItems() != null) {
            items = order.getItems().stream()
                    .map(this::toItemResponse)
                    .toList();
        }
        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNo(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                order.getRemark(),
                items,
                order.getCreatedAt() != null ? order.getCreatedAt().toString() : ""
        );
    }

    private AdminOrderResponse toAdminOrderResponse(OrderEntity order) {
        if (order == null) return null;
        List<OrderItemResponse> items = null;
        if (order.getItems() != null) {
            items = order.getItems().stream()
                    .map(this::toItemResponse)
                    .toList();
        }
        return new AdminOrderResponse(
                order.getId(),
                order.getUserId(),
                order.getOrderNo(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                order.getRemark(),
                items,
                order.getCreatedAt() != null ? order.getCreatedAt().toString() : ""
        );
    }

    private AdminOrderDetailResponse toAdminOrderDetailResponse(OrderEntity order) {
        if (order == null) return null;
        List<OrderItemResponse> items = null;
        if (order.getItems() != null) {
            items = order.getItems().stream()
                    .map(this::toItemResponse)
                    .toList();
        }
        return new AdminOrderDetailResponse(
                order.getId(),
                order.getUserId(),
                order.getOrderNo(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                order.getRemark(),
                items,
                order.getCreatedAt() != null ? order.getCreatedAt().toString() : "",
                order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : ""
        );
    }

    private OrderItemResponse toItemResponse(OrderItemEntity item) {
        if (item == null) return null;
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getProductThumbnail(),
                item.getPrice(),
                item.getQuantity()
        );
    }

    public record OrderResponse(
            Integer id,
            String orderNo,
            BigDecimal totalAmount,
            String status,
            List<OrderItemResponse> items,
            String createdAt
    ) {}

    public record OrderDetailResponse(
            Integer id,
            String orderNo,
            BigDecimal totalAmount,
            String status,
            String receiverName,
            String receiverPhone,
            String receiverAddress,
            String remark,
            List<OrderItemResponse> items,
            String createdAt
    ) {}

    public record AdminOrderResponse(
            Integer id,
            Integer userId,
            String orderNo,
            BigDecimal totalAmount,
            String status,
            String receiverName,
            String receiverPhone,
            String receiverAddress,
            String remark,
            List<OrderItemResponse> items,
            String createdAt
    ) {}

    public record AdminOrderDetailResponse(
            Integer id,
            Integer userId,
            String orderNo,
            BigDecimal totalAmount,
            String status,
            String receiverName,
            String receiverPhone,
            String receiverAddress,
            String remark,
            List<OrderItemResponse> items,
            String createdAt,
            String updatedAt
    ) {}

    public record OrderItemResponse(
            Integer id,
            Integer productId,
            String productName,
            String productThumbnail,
            BigDecimal price,
            Integer quantity
    ) {}

    public record UpdateStatusRequest(String status) {}
}
