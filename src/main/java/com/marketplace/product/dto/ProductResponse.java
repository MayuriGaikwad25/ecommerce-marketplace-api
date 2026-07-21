package com.marketplace.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        String sku,
        boolean active,
        String categoryId,
        String categoryName,
        String vendorId,
        Instant createdAt) {}
