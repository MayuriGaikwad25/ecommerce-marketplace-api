package com.marketplace.category.service;

import com.marketplace.category.dto.CategoryRequest;
import com.marketplace.category.dto.CategoryResponse;
import java.util.List;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    CategoryResponse getById(String id);

    List<CategoryResponse> list(String parentId);

    CategoryResponse update(String id, CategoryRequest request);

    void delete(String id);
}
