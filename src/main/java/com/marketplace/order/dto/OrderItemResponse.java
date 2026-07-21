package com.marketplace.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        String productId, String productName, Integer quantity, BigDecimal priceAtPurchase, BigDecimal lineTotal) {}
