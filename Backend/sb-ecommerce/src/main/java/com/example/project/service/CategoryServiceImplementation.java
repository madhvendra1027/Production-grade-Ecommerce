package com.example.project.service;

import com.example.project.globalExceptionHandler.APIexception;
import com.example.project.globalExceptionHandler.ResourceNotFoundException;
import com.example.project.model.Category;
import com.example.project.payLoad.CategoryDTO;
import com.example.project.payLoad.CategoryResponse;
import com.example.project.repositories.CategoryRepositories;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service   //to let the spring know to create a bean and manage it otherwise it would throw an error
public class CategoryServiceImplementation implements CategoryService {

    @Autowired
    private CategoryRepositories categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public CategoryResponse getCategories(Integer pageNumber, Integer pageSize, String sort_by, String sort_order) {

        Sort sortByOrder = sort_order.equalsIgnoreCase("asc")
                ?Sort.by(sort_by).ascending()
                :Sort.by(sort_by).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByOrder);
        Page<Category> categoryPage = categoryRepository.findAll(pageDetails);
        List<Category> categoryList = categoryPage.getContent();

        // No longer throws APIexception("No category exists") on an empty result.
        // This now backs a public endpoint, and a shop with no categories yet is not
        // a client error - it was returning HTTP 400 for an empty catalogue.

        List<CategoryDTO> categoryDTOS = categoryList.stream()
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                .toList();

        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setContent(categoryDTOS);
        categoryResponse.setPageNumber(categoryPage.getNumber());
        categoryResponse.setPageSize(categoryPage.getSize());
        // Was getNumberOfElements(), which is the count on the current page, not the
        // total across all pages. The UI uses this to size its pager, so it under-
        // reported the number of categories.
        categoryResponse.setTotalElements(categoryPage.getTotalElements());
        categoryResponse.setTotalPages(categoryPage.getTotalPages());
        categoryResponse.setLastPage(categoryPage.isLast());
        return categoryResponse;
    }

    @Override
    @Transactional
    public CategoryDTO addCategory(CategoryDTO categoryDTO){
        Category category = modelMapper.map(categoryDTO, Category.class);

        // Ignore any id supplied on a create, so a POST cannot overwrite an existing
        // category row.
        category.setCategoryID(null);

        Category categoryFromDB = categoryRepository.findByCategoryName(category.getCategoryName());
        if(categoryFromDB != null){
            throw new APIexception("Category with the category name " + category.getCategoryName() + " is already present");
        }

        Category savedCategory = categoryRepository.save(category);
        return modelMapper.map(savedCategory, CategoryDTO.class);
    }

    @Override
    @Transactional
    public CategoryDTO deleteCategory(Long categoryID){
        Category category = categoryRepository.findById(categoryID)
                        .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryID));

        // Note: Category.products is mapped with CascadeType.ALL, so deleting a
        // category deletes its products too. That is a destructive operation -
        // consider blocking deletion while products still reference the category.
        categoryRepository.delete(category);
        return modelMapper.map(category, CategoryDTO.class);
    }

    @Override
    @Transactional
    public CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryID){
        Category savedCategory = categoryRepository.findById(categoryID)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryID));

        // Mutate the loaded entity instead of saving a fresh one built from the DTO.
        // The old version mapped the DTO to a brand new Category, stamped the id on it
        // and saved that - which means every field the DTO does not carry (here, the
        // products collection, mapped with CascadeType.ALL) was written as null.
        savedCategory.setCategoryName(categoryDTO.getCategoryName());

        Category updated = categoryRepository.save(savedCategory);
        return modelMapper.map(updated, CategoryDTO.class);
    }
}
