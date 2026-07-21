package com.marketplace.category.service.impl;

import com.marketplace.category.dto.CategoryRequest;
import com.marketplace.category.dto.CategoryResponse;
import com.marketplace.category.entity.Category;
import com.marketplace.category.mapper.CategoryMapper;
import com.marketplace.category.repository.CategoryRepository;
import com.marketplace.category.service.CategoryService;
import com.marketplace.common.exception.DuplicateResourceException;
import com.marketplace.common.exception.ResourceInUseException;
import com.marketplace.common.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Category already exists: " + request.name());
        }
        Category parent = resolveParent(request.parentId());

        Category category = Category.builder().name(request.name()).parent(parent).build();
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse getById(String id) {
        return categoryMapper.toResponse(findOrThrow(id));
    }

    @Override
    public List<CategoryResponse> list(String parentId) {
        List<Category> categories = parentId == null
                ? categoryRepository.findAllByParentIdIsNull()
                : categoryRepository.findAllByParentId(parentId);
        return categories.stream().map(categoryMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public CategoryResponse update(String id, CategoryRequest request) {
        Category category = findOrThrow(id);

        if (!category.getName().equals(request.name()) && categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Category already exists: " + request.name());
        }

        category.setName(request.name());
        category.setParent(resolveParent(request.parentId()));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Category category = findOrThrow(id);

        if (categoryRepository.existsByParentId(id)) {
            throw new ResourceInUseException("Cannot delete a category that has subcategories");
        }

        categoryRepository.delete(category);
    }

    private Category resolveParent(String parentId) {
        return parentId == null ? null : findOrThrow(parentId);
    }

    private Category findOrThrow(String id) {
        return categoryRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }
}
