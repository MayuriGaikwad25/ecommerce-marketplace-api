package com.marketplace.order.dto;

import jakarta.validation.constraints.NotBlank;

public record PlaceOrderRequest(@NotBlank String shippingAddress) {}
