package com.ecommerce.project.services.impl;

import com.ecommerce.project.dto.CategoryDTO;
import com.ecommerce.project.dto.CategoryResponse;
import com.ecommerce.project.entities.Category;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.services.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    private final ModelMapper modelMapper;

    @Override
    public CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        log.debug("CategoryService.getAllCategories call started...");
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Category> categoryPage = categoryRepository.findAll(pageDetails);
        List<Category> categories = categoryPage.getContent();
        List<CategoryDTO> categoryDTOS = categories.stream()
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                .toList();
        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setContent(categoryDTOS);
        categoryResponse.setPageNumber(categoryPage.getNumber());
        categoryResponse.setPageSize(categoryPage.getSize());
        categoryResponse.setTotalPages(categoryPage.getTotalPages());
        categoryResponse.setTotalElements(categoryPage.getTotalElements());
        categoryResponse.setLastPage(categoryPage.isLast());
        log.debug("CategoryService.getAllCategories call completed...");
        return categoryResponse;
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        log.debug("CategoryService.createCategory call started...");
        Category category = modelMapper.map(categoryDTO, Category.class);
        Category savedCategory = categoryRepository.findByCategoryName(category.getCategoryName());
        if (savedCategory != null) {
            log.debug("CategoryService.createCategory call failed...");
            throw new APIException("Category already exists.");
        }
        savedCategory = categoryRepository.save(category);
        log.debug("CategoryService.createCategory call completed...");
        return modelMapper.map(savedCategory, CategoryDTO.class);
    }

    @Override
    public CategoryDTO deleteCategory(Long categoryId) {
        log.debug("CategoryService.deleteCategory call started...");
        Category category =
                categoryRepository.findById(categoryId)
                        .orElse(null);
        if (Objects.isNull(category)) {
            log.debug("CategoryService.deleteCategory call failed...");
            throw new ResourceNotFoundException("Category not found");
        }
        categoryRepository.delete(category);
        log.debug("CategoryService.deleteCategory call completed...");
        return modelMapper.map(category, CategoryDTO.class);
    }

    @Override
    public CategoryDTO updateCategory(Long categoryId, CategoryDTO categoryDTO) {
        log.debug("CategoryService.updateCategory call started...");
        Category category = modelMapper.map(categoryDTO, Category.class);
        Category existingCategory =
                categoryRepository.findById(categoryId)
                        .orElse(null);
        if (Objects.isNull(existingCategory)) {
            log.debug("CategoryService.updateCategory call failed...");
            throw new ResourceNotFoundException("Category not found");
        }
        existingCategory.setCategoryName(category.getCategoryName());
        log.debug("CategoryService.updateCategory call completed...");
        return modelMapper.map(categoryRepository.save(existingCategory), CategoryDTO.class);
    }
}
