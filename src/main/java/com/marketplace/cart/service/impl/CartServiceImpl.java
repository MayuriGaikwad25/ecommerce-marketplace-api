package com.marketplace.cart.service.impl;

import com.marketplace.cart.dto.AddCartItemRequest;
import com.marketplace.cart.dto.CartItemResponse;
import com.marketplace.cart.dto.CartResponse;
import com.marketplace.cart.dto.UpdateCartItemRequest;
import com.marketplace.cart.entity.Cart;
import com.marketplace.cart.entity.CartItem;
import com.marketplace.cart.repository.CartRepository;
import com.marketplace.cart.service.CartService;
import com.marketplace.common.exception.InsufficientStockException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.product.entity.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(String customerEmail) {
        return toResponse(getOrCreateCart(customerEmail));
    }

    @Override
    @Transactional
    public CartResponse addItem(String customerEmail, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(customerEmail);
        Product product = findProduct(request.productId());

        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        int newQuantity = (existing == null ? 0 : existing.getQuantity()) + request.quantity();
        assertStockAvailable(product, newQuantity);

        if (existing != null) {
            existing.setQuantity(newQuantity);
        } else {
            cart.getItems()
                    .add(CartItem.builder().cart(cart).product(product).quantity(newQuantity).build());
        }

        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItem(String customerEmail, String productId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(customerEmail);
        CartItem item = findItem(cart, productId);
        assertStockAvailable(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(String customerEmail, String productId) {
        Cart cart = getOrCreateCart(customerEmail);
        CartItem item = findItem(cart, productId);
        cart.getItems().remove(item);
        return toResponse(cart);
    }

    private Cart getOrCreateCart(String customerEmail) {
        User user = userRepository
                .findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + customerEmail));
        return cartRepository
                .findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    private CartItem findItem(Cart cart, String productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product not in cart: " + productId));
    }

    private Product findProduct(String productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }

    private void assertStockAvailable(Product product, int requestedQuantity) {
        if (product.getStockQuantity() < requestedQuantity) {
            throw new InsufficientStockException("Not enough stock available for " + product.getName());
        }
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> new CartItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getPrice(),
                        item.getQuantity(),
                        item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))))
                .toList();

        BigDecimal total = items.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, total);
    }
}
