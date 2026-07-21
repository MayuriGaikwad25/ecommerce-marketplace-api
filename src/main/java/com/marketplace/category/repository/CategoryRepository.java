package com.marketplace.category.repository;

import com.marketplace.category.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, String> {

    boolean existsByName(String name);

    boolean existsByParentId(String parentId);

    List<Category> findAllByParentIdIsNull();

    List<Category> findAllByParentId(String parentId);
}
