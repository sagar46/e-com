package com.ecommerce.project.repositories;

import com.ecommerce.project.entities.Category;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Category findByCategoryName(@NotBlank(message = "Category name must not be null") String categoryName);
}
