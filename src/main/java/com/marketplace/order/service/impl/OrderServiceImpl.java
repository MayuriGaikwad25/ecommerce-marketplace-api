package com.marketplace.order.service.impl;

import com.marketplace.cart.entity.Cart;
import com.marketplace.cart.entity.CartItem;
import com.marketplace.cart.repository.CartRepository;
import com.marketplace.common.dto.PageResponse;
import com.marketplace.common.exception.EmptyCartException;
import com.marketplace.common.exception.InsufficientStockException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.order.dto.OrderItemResponse;
import com.marketplace.order.dto.OrderResponse;
import com.marketplace.order.dto.PlaceOrderRequest;
import com.marketplace.order.dto.VendorOrderItemResponse;
import com.marketplace.order.entity.Order;
import com.marketplace.order.entity.OrderItem;
import com.marketplace.order.entity.OrderStatus;
import com.marketplace.order.repository.OrderItemRepository;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.order.service.OrderService;
import com.marketplace.product.entity.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OrderResponse placeOrder(String customerEmail, PlaceOrderRequest request) {
        User customer = findUser(customerEmail);
        Cart cart = cartRepository
                .findByUserId(customer.getId())
                .orElseThrow(() -> new EmptyCartException("Your cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException("Your cart is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // Re-read each product inside this transaction and decrement stock. Hibernate's
        // dirty-checking flushes the change on commit as
        // UPDATE products SET stock_quantity=?, version=?+1 WHERE id=? AND version=?
        // If another transaction already changed this product's version in between,
        // zero rows match and Hibernate throws OptimisticLockException — translated to a
        // 409 by GlobalExceptionHandler. That's what stops two customers from both buying
        // the last unit of stock.
        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository
                    .findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: " + cartItem.getProduct().getId()));

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException("Not enough stock for " + product.getName());
            }

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            orderItems.add(OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(product.getPrice())
                    .build());
        }

        Order order = Order.builder()
                .user(customer)
                .status(OrderStatus.PLACED)
                .totalAmount(totalAmount)
                .shippingAddress(request.shippingAddress())
                .build();
        orderItems.forEach(item -> item.setOrder(order));
        order.setItems(orderItems);

        Order saved = orderRepository.save(order);
        cart.getItems().clear();

        log.info(
                "Order {} placed by {} for {} ({} item(s))",
                saved.getId(),
                customerEmail,
                totalAmount,
                orderItems.size());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(String customerEmail, String orderId) {
        User customer = findUser(customerEmail);
        Order order = orderRepository
                .findById(orderId)
                .filter(o -> o.getUser().getId().equals(customer.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listMyOrders(String customerEmail, Pageable pageable) {
        User customer = findUser(customerEmail);
        Page<OrderResponse> page = orderRepository.findAllByUserId(customer.getId(), pageable).map(this::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VendorOrderItemResponse> listOrderItemsForVendor(String vendorEmail, Pageable pageable) {
        User vendor = findUser(vendorEmail);
        Page<VendorOrderItemResponse> page = orderItemRepository
                .findAllByProduct_Vendor_Id(vendor.getId(), pageable)
                .map(item -> new VendorOrderItemResponse(
                        item.getOrder().getId(),
                        item.getOrder().getStatus(),
                        item.getProduct().getId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPriceAtPurchase(),
                        item.getOrder().getCreatedAt()));
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listAllOrders(Pageable pageable) {
        return PageResponse.from(orderRepository.findAll(pageable).map(this::toResponse));
    }

    private User findUser(String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPriceAtPurchase(),
                        item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()))))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getShippingAddress(),
                items,
                order.getCreatedAt());
    }
}
