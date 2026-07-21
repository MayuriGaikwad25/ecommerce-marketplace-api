package com.marketplace.product.mapper;

import com.marketplace.product.dto.ProductResponse;
import com.marketplace.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "vendorId", source = "vendor.id")
    ProductResponse toResponse(Product product);
}
