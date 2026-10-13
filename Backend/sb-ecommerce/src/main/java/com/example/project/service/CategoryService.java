package com.example.project.service;

//all the buisness logic should reside in the service part.

import com.example.project.model.Category;
import com.example.project.payLoad.CategoryDTO;
import com.example.project.payLoad.CategoryResponse;

//why interface? to promote loose coupling.
public interface CategoryService {

    CategoryResponse getCategories(Integer pageNumber, Integer pageSize, String sort_by, String sort_order);
    CategoryDTO addCategory(CategoryDTO categoryDTO);
    CategoryDTO deleteCategory(Long categoryID);
    CategoryDTO updateCategory(CategoryDTO categoryDTOS, Long categoryID);
}
