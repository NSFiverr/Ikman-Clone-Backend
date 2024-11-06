package com.marketplace.platform.service.category;

import com.marketplace.platform.domain.category.Category;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.dto.request.CreateCategoryRequest;
import com.marketplace.platform.dto.request.UpdateCategoryRequest;
import com.marketplace.platform.dto.response.CategoryDetailResponse;
import com.marketplace.platform.dto.response.CategoryResponse;
import com.marketplace.platform.dto.response.CategoryAttributeResponse;
import com.marketplace.platform.dto.response.UserResponse;
import com.marketplace.platform.exception.ResourceNotFoundException;
import com.marketplace.platform.repository.category.CategoryRepository;

import com.marketplace.platform.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::convertToCategoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDetailResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return convertToCategoryDetailResponse(category);
    }


    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());

        UserResponse userResponse=userService.getUserById(request.getCreatedById());
        User user=new User();
        user.setUserId(userResponse.getUserId());
        user.setEmail(userResponse.getEmail());
        category.setCreatedBy(user);
        //category.setCreatedBy(userService.getUserById(request.getCreatedById()));
        return convertToCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        return convertToCategoryResponse(categoryRepository.save(category));
    }

    private CategoryResponse convertToCategoryResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setCategoryId(category.getCategoryId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setCreatedById(category.getCreatedBy().getUserId());
        response.setCreatedByUsername(category.getCreatedBy().getEmail());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
        return response;
    }



    private CategoryDetailResponse convertToCategoryDetailResponse(Category category) {
        CategoryDetailResponse response = new CategoryDetailResponse();
        // Copy basic category properties
        this.convertToCategoryResponse(category, response);

        // Add attributes
        response.setAttributes(category.getAttributes().stream()
                .map(this::convertToCategoryAttributeResponse)
                .collect(Collectors.toList()));

        return response;
    }


    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        categoryRepository.delete(category);
    }

    private void convertToCategoryResponse(Category category, CategoryResponse response) {
        response.setCategoryId(category.getCategoryId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setCreatedById(category.getCreatedBy().getUserId());
        response.setCreatedByUsername(category.getCreatedBy().getEmail());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
    }

    private CategoryAttributeResponse convertToCategoryAttributeResponse(com.marketplace.platform.domain.category.CategoryAttribute attribute) {
        CategoryAttributeResponse response = new CategoryAttributeResponse();
        response.setCategoryId(attribute.getCategory().getCategoryId());
        response.setAttrDefId(attribute.getAttributeDefinition().getAttrDefId());
        response.setAttributeName(attribute.getAttributeDefinition().getName());
        response.setAttributeDisplayName(attribute.getAttributeDefinition().getDisplayName());
        response.setDataType(attribute.getAttributeDefinition().getDataType());
        response.setIsRequired(attribute.getIsRequired());
        response.setDisplayOrder(attribute.getDisplayOrder());
        response.setCreatedAt(attribute.getCreatedAt());
        return response;
    }
}


