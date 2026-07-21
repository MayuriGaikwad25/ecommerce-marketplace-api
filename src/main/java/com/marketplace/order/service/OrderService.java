package com.marketplace.order.service;

import com.marketplace.common.dto.PageResponse;
import com.marketplace.order.dto.OrderResponse;
import com.marketplace.order.dto.PlaceOrderRequest;
import com.marketplace.order.dto.VendorOrderItemResponse;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse placeOrder(String customerEmail, PlaceOrderRequest request);

    OrderResponse getById(String customerEmail, String orderId);

    PageResponse<OrderResponse> listMyOrders(String customerEmail, Pageable pageable);

    PageResponse<VendorOrderItemResponse> listOrderItemsForVendor(String vendorEmail, Pageable pageable);

    PageResponse<OrderResponse> listAllOrders(Pageable pageable);
}
