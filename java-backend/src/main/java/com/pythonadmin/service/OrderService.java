package com.pythonadmin.service;

import com.pythonadmin.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class OrderService {
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final ProductRepository productRepo;
    private final CartRepository cartRepo;

    public OrderService(OrderRepository orderRepo, OrderItemRepository orderItemRepo,
                        ProductRepository productRepo, CartRepository cartRepo) {
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.productRepo = productRepo;
        this.cartRepo = cartRepo;
    }

    public Page<OrderEntity> listOrders(Integer userId, Pageable pageable) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public OrderEntity getOrderDetail(Integer userId, Integer orderId) {
        OrderEntity order = orderRepo.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "订单不存在"));
        // 加载订单项
        List<OrderItemEntity> items = orderItemRepo.findByOrderId(orderId);
        order.setItems(items);
        return order;
    }

    @Transactional
    public OrderEntity createOrder(Integer userId, CreateOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "订单商品不能为空");
        }

        OrderEntity order = new OrderEntity();
        order.setUserId(userId);
        order.setOrderNo(generateOrderNo());
        order.setReceiverName(request.receiverName());
        order.setReceiverPhone(request.receiverPhone());
        order.setReceiverAddress(request.receiverAddress());
        order.setRemark(request.remark());
        order.setStatus("pending");
        order.setTotalAmount(BigDecimal.ZERO);

        // 保存订单
        order = orderRepo.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 创建订单项
        for (OrderItemRequest itemReq : request.items()) {
            ProductEntity product = productRepo.findById(itemReq.productId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "产品不存在: " + itemReq.productId()));

            if (product.getStatus() != null && !product.getStatus()) {
                throw new ResponseStatusException(BAD_REQUEST, "产品已下架: " + product.getName());
            }

            BigDecimal price = product.getActualPrice() != null ? product.getActualPrice() : product.getOfficialPrice();
            int quantity = itemReq.quantity() != null ? itemReq.quantity() : 1;
            BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(quantity));

            OrderItemEntity item = new OrderItemEntity();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductThumbnail(product.getThumbnail());
            item.setPrice(price);
            item.setQuantity(quantity);
            orderItemRepo.save(item);

            totalAmount = totalAmount.add(itemTotal);
        }

        order.setTotalAmount(totalAmount);
        order.setUpdatedAt(Instant.now());
        return orderRepo.save(order);
    }

    @Transactional
    public OrderEntity confirmPay(Integer userId, Integer orderId) {
        OrderEntity order = orderRepo.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "订单不存在"));

        if (!"pending".equals(order.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "订单状态不允许支付");
        }

        order.setStatus("paid");
        order.setUpdatedAt(Instant.now());
        return orderRepo.save(order);
    }

    // 管理员接口
    public Page<OrderEntity> listAllOrders(String keyword, String status, Pageable pageable) {
        return orderRepo.findAll(pageable);
    }

    public OrderEntity getOrderById(Integer orderId) {
        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "订单不存在"));
        List<OrderItemEntity> items = orderItemRepo.findByOrderId(orderId);
        order.setItems(items);
        return order;
    }

    @Transactional
    public OrderEntity updateOrderStatus(Integer orderId, String status) {
        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "订单不存在"));
        order.setStatus(status);
        order.setUpdatedAt(Instant.now());
        return orderRepo.save(order);
    }

    private String generateOrderNo() {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(java.time.LocalDateTime.now());
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD" + timestamp + random;
    }

    public record CreateOrderRequest(
            List<OrderItemRequest> items,
            String receiverName,
            String receiverPhone,
            String receiverAddress,
            String remark
    ) {}

    public record OrderItemRequest(
            Integer productId,
            Integer quantity,
            BigDecimal price
    ) {}
}
