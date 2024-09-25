package com.ecommerce.project.controllers;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.dto.CategoryDTO;
import com.ecommerce.project.dto.CategoryResponse;
import com.ecommerce.project.services.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/public/categories")
    public ResponseEntity<CategoryResponse> getAllCategories(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CATEGORIES_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder
    ) {
        log.debug("CategoryService.getAllCategories call started...");
        CategoryResponse categoryResponse = categoryService.getAllCategories(pageNumber, pageSize, sortBy, sortOrder);
        log.debug("CategoryService.getAllCategories call completed...");
        return ResponseEntity.status(HttpStatus.OK).body(categoryResponse);
    }

    @PostMapping("/admin/categories")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        log.debug("CategoryService.createCategory call started...");
        CategoryDTO savedCategory = categoryService.createCategory(categoryDTO);
        log.debug("CategoryService.createCategory call completed...");
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
    }

    @PostMapping("/admin/categories/all")
    public ResponseEntity<String> createCategoryAll() {
        log.debug("CategoryService.createCategoryAll call started...");
        for (int i = 1; i <= 30; i++) {
            CategoryDTO category = new CategoryDTO();
            category.setCategoryName("Category " + i);
            CategoryDTO savedCategory = categoryService.createCategory(category);
        }
        log.debug("CategoryService.createCategoryAll call completed...");
        return ResponseEntity.status(HttpStatus.CREATED).body("created");
    }

    @DeleteMapping("/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> deleteCategory(@PathVariable Long categoryId) {
        log.debug("CategoryService.deleteCategory call started...");
        CategoryDTO deletedCategory = categoryService.deleteCategory(categoryId);
        log.debug("CategoryService.deleteCategory call completed...");
        return ResponseEntity.status(HttpStatus.OK).body(deletedCategory);
    }

    @PutMapping("/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryDTO categoryDTO) {
        log.debug("CategoryService.updateCategory call started...");
        CategoryDTO updatedCategory = categoryService.updateCategory(categoryId, categoryDTO);
        log.debug("CategoryService.updateCategory call completed...");
        return ResponseEntity.status(HttpStatus.OK).body(updatedCategory);
    }


}
