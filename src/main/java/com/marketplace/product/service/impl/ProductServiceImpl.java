package com.marketplace.product.service.impl;

import com.marketplace.category.entity.Category;
import com.marketplace.category.repository.CategoryRepository;
import com.marketplace.common.dto.PageResponse;
import com.marketplace.common.exception.DuplicateResourceException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.product.dto.ProductRequest;
import com.marketplace.product.dto.ProductResponse;
import com.marketplace.product.entity.Product;
import com.marketplace.product.mapper.ProductMapper;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.product.service.ProductService;
import com.marketplace.product.spec.ProductSpecifications;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse create(String vendorEmail, ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("SKU already in use: " + request.sku());
        }

        User vendor = findVendor(vendorEmail);
        Category category = findCategory(request.categoryId());

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .sku(request.sku())
                .active(true)
                .category(category)
                .vendor(vendor)
                .build();

        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(String id) {
        return productMapper.toResponse(findProduct(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(
            String categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.isActive(),
                ProductSpecifications.hasCategory(categoryId),
                ProductSpecifications.priceGreaterThanOrEqual(minPrice),
                ProductSpecifications.priceLessThanOrEqual(maxPrice));

        Page<ProductResponse> page = productRepository.findAll(spec, pageable).map(productMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional
    public ProductResponse update(String vendorEmail, String productId, ProductRequest request) {
        Product product = findProduct(productId);
        assertOwnership(product, vendorEmail);

        if (!product.getSku().equals(request.sku()) && productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("SKU already in use: " + request.sku());
        }

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setSku(request.sku());
        product.setCategory(findCategory(request.categoryId()));

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public void deactivate(String vendorEmail, String productId) {
        Product product = findProduct(productId);
        assertOwnership(product, vendorEmail);
        product.setActive(false);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listMyProducts(String vendorEmail, Pageable pageable) {
        User vendor = findVendor(vendorEmail);
        Page<ProductResponse> page =
                productRepository.findAllByVendorId(vendor.getId(), pageable).map(productMapper::toResponse);
        return PageResponse.from(page);
    }

    private void assertOwnership(Product product, String vendorEmail) {
        if (!product.getVendor().getEmail().equals(vendorEmail)) {
            throw new AccessDeniedException("You do not own this product");
        }
    }

    private User findVendor(String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + email));
    }

    private Category findCategory(String categoryId) {
        return categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }

    private Product findProduct(String id) {
        return productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }
}
