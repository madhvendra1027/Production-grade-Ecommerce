package com.example.project.controller;

import com.example.project.configuration.AppConstants;
import com.example.project.payLoad.CategoryDTO;
import com.example.project.payLoad.CategoryResponse;
import com.example.project.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Public category listing.
     *
     * This endpoint did not exist. The only GET was /api/admin/categories, which is
     * ADMIN gated, so the storefront's category filter could never load for a normal
     * shopper - and after the frontend was pointed at /public/categories it started
     * matching the POST mapping below instead and returning 405.
     */
    @GetMapping("/public/categories")
    public ResponseEntity<CategoryResponse> getPublicCategories(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false)Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false)Integer pageSize,
            @RequestParam(name = "sort_by", defaultValue = AppConstants.SORT_BY_CATEGORY, required = false)String sort_by,
            @RequestParam(name = "sort_order", defaultValue = AppConstants.SORT_ORDER, required = false)String sort_order)
    {
        CategoryResponse categoryResponse = categoryService.getCategories(pageNumber, pageSize, sort_by, sort_order);
        return new ResponseEntity<>(categoryResponse, HttpStatus.OK);
    }

    /** Kept for the admin dashboard; identical data to the public listing. */
    @GetMapping("/admin/categories")
    public ResponseEntity<CategoryResponse> getCategories(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false)Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false)Integer pageSize,
            @RequestParam(name = "sort_by", defaultValue = AppConstants.SORT_BY_CATEGORY, required = false)String sort_by,
            @RequestParam(name = "sort_order", defaultValue = AppConstants.SORT_ORDER, required = false)String sort_order)
    {
        CategoryResponse categoryResponse = categoryService.getCategories(pageNumber, pageSize, sort_by, sort_order);
        return new ResponseEntity<>(categoryResponse, HttpStatus.OK);
    }

    /**
     * Moved from POST /api/public/categories to POST /api/admin/categories.
     *
     * /api/public/** is permitAll, so category creation was wide open - any
     * unauthenticated caller could insert rows into the categories table.
     */
    @PostMapping("/admin/categories")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO){
        CategoryDTO savedCategoryDTOS = categoryService.addCategory(categoryDTO);
        return new ResponseEntity<>(savedCategoryDTOS, HttpStatus.CREATED);
    }

    @DeleteMapping("/admin/categories/{categoryID}")
    public ResponseEntity<CategoryDTO> deleteCategory(@PathVariable Long categoryID){
        CategoryDTO deletedCategoryDTO = categoryService.deleteCategory(categoryID);
        return new ResponseEntity<>(deletedCategoryDTO, HttpStatus.OK);
    }

    @PutMapping("/admin/categories/{categoryID}")
    public ResponseEntity<CategoryDTO> updateCategory(@Valid @RequestBody CategoryDTO categoryDTOS,
                                                      @PathVariable Long categoryID) {
        CategoryDTO updatedCategoryDTOS = categoryService.updateCategory(categoryDTOS, categoryID);
        return new ResponseEntity<>(updatedCategoryDTOS, HttpStatus.OK);
    }
}
