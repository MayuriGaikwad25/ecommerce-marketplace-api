package com.marketplace.product.service;

import com.marketplace.common.dto.PageResponse;
import com.marketplace.product.dto.ProductRequest;
import com.marketplace.product.dto.ProductResponse;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse create(String vendorEmail, ProductRequest request);

    ProductResponse getById(String id);

    PageResponse<ProductResponse> search(
            String categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    ProductResponse update(String vendorEmail, String productId, ProductRequest request);

    void deactivate(String vendorEmail, String productId);

    PageResponse<ProductResponse> listMyProducts(String vendorEmail, Pageable pageable);
}
