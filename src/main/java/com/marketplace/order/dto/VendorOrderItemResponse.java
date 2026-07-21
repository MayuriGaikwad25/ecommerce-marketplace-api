package com.marketplace.order.dto;

import com.marketplace.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record VendorOrderItemResponse(
        String orderId,
        OrderStatus orderStatus,
        String productId,
        String productName,
        Integer quantity,
        BigDecimal priceAtPurchase,
        Instant orderCreatedAt) {}
