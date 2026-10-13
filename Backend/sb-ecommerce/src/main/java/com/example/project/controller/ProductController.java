package com.example.project.controller;

import com.example.project.configuration.AppConstants;
import com.example.project.payLoad.ProductDTO;
import com.example.project.payLoad.ProductResponse;
import com.example.project.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/admin/categories/{categoryId}/product")
    public ResponseEntity<ProductDTO> addProduct(@Valid @RequestBody ProductDTO productDTO, @PathVariable Long categoryId){
        ProductDTO savedProductDTO = productService.addProduct(productDTO, categoryId);
        return new ResponseEntity<>(savedProductDTO, HttpStatus.CREATED);
    }

    @GetMapping("/public/products")
    public ResponseEntity<ProductResponse> getAllProducts(
            @RequestParam(name = "keyword", required = false)String keyword,
            @RequestParam(name = "category", required = false)String category,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false)Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false)Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_BY_PRODUCT, required = false)String sort_by,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER, required = false)String sort_order)
    {
        ProductResponse productResponse = productService.getAllProducts(pageNumber, pageSize, sort_by, sort_order, keyword, category);
        return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }

    @GetMapping("/public/categories/{categoryId}/products")
    public ResponseEntity<ProductResponse> getByCategory(@PathVariable Long categoryId,
                                                         @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false)Integer pageNumber,
                                                         @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false)Integer pageSize,
                                                         @RequestParam(name = "sort_by", defaultValue = AppConstants.SORT_BY_PRODUCT, required = false)String sort_by,
                                                         @RequestParam(name = "sort_order", defaultValue = AppConstants.SORT_ORDER, required = false)String sort_order){
        ProductResponse productResponse = productService.searchByCategory(categoryId, pageNumber, pageSize, sort_by, sort_order);
        return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }

    @GetMapping("/public/products/keyword/{keyword}")
    public ResponseEntity<ProductResponse> getByKeyword(@PathVariable String keyword,
                                                        @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false)Integer pageNumber,
                                                        @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false)Integer pageSize,
                                                        @RequestParam(name = "sort_by", defaultValue = AppConstants.SORT_BY_PRODUCT, required = false)String sort_by,
                                                        @RequestParam(name = "sort_order", defaultValue = AppConstants.SORT_ORDER, required = false)String sort_order){
        ProductResponse productResponse = productService.searchByKeyword(keyword, pageNumber, pageSize, sort_by, sort_order);
        // Was HttpStatus.FOUND (302). 302 is a redirect; a successful search is 200.
        return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }

    @PutMapping("/admin/products/{productId}")
    public ResponseEntity<ProductDTO> updateProduct(@Valid @RequestBody ProductDTO productDTO, @PathVariable Long productId){
        ProductDTO updatedProductDTO = productService.update(productDTO, productId);
        return new ResponseEntity<>(updatedProductDTO, HttpStatus.OK);
    }

    @DeleteMapping("/admin/products/{productId}")
    public ResponseEntity<ProductDTO> deleteProduct(@PathVariable Long productId){
        ProductDTO deletedProductDTO = productService.deleteProduct(productId);
        return new ResponseEntity<>(deletedProductDTO, HttpStatus.OK);
    }

    @PutMapping("/admin/products/{productId}/image")
    public ResponseEntity<ProductDTO> updateProductImage(@PathVariable Long productId, @RequestParam("image")MultipartFile image) throws IOException {
        ProductDTO updatedImageProductDTO = productService.updateImage(productId, image);
        return new ResponseEntity<>(updatedImageProductDTO, HttpStatus.OK);
    }

    @GetMapping("/admin/products")
    public ResponseEntity<ProductResponse> getAllProductsForAdmin(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false)Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false)Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_BY_PRODUCT, required = false)String sort_by,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER, required = false)String sort_order)
    {
        ProductResponse productResponse = productService.getAllProductsForAdmin(pageNumber, pageSize, sort_by, sort_order);
        return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }

    @GetMapping("/seller/products")
    public ResponseEntity<ProductResponse> getAllProductsForSeller(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false)Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false)Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_BY_PRODUCT, required = false)String sort_by,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER, required = false)String sort_order)
    {
        ProductResponse productResponse = productService.getAllProductsForSeller(pageNumber, pageSize, sort_by, sort_order);
        return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }
}
