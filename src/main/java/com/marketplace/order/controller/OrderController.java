package com.marketplace.order.controller;

import com.marketplace.common.dto.PageResponse;
import com.marketplace.order.dto.OrderResponse;
import com.marketplace.order.dto.PlaceOrderRequest;
import com.marketplace.order.dto.VendorOrderItemResponse;
import com.marketplace.order.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Checkout, order history, vendor and admin order views")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse placeOrder(@Valid @RequestBody PlaceOrderRequest request, Authentication authentication) {
        return orderService.placeOrder(authentication.getName(), request);
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public PageResponse<OrderResponse> listMyOrders(
            @PageableDefault(size = 20) Pageable pageable, Authentication authentication) {
        return orderService.listMyOrders(authentication.getName(), pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse getById(@PathVariable String id, Authentication authentication) {
        return orderService.getById(authentication.getName(), id);
    }

    @GetMapping("/vendor/items")
    @PreAuthorize("hasRole('VENDOR')")
    public PageResponse<VendorOrderItemResponse> listOrderItemsForVendor(
            @PageableDefault(size = 20) Pageable pageable, Authentication authentication) {
        return orderService.listOrderItemsForVendor(authentication.getName(), pageable);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<OrderResponse> listAllOrders(@PageableDefault(size = 20) Pageable pageable) {
        return orderService.listAllOrders(pageable);
    }
}
