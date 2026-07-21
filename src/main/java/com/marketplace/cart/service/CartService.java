package com.marketplace.cart.service;

import com.marketplace.cart.dto.AddCartItemRequest;
import com.marketplace.cart.dto.CartResponse;
import com.marketplace.cart.dto.UpdateCartItemRequest;

public interface CartService {

    CartResponse getCart(String customerEmail);

    CartResponse addItem(String customerEmail, AddCartItemRequest request);

    CartResponse updateItem(String customerEmail, String productId, UpdateCartItemRequest request);

    CartResponse removeItem(String customerEmail, String productId);
}
