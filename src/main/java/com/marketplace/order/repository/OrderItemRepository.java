package com.marketplace.order.repository;

import com.marketplace.order.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, String> {

    Page<OrderItem> findAllByProduct_Vendor_Id(String vendorId, Pageable pageable);
}
