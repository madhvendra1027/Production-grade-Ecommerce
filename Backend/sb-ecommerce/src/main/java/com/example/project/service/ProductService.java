package com.example.project.service;

import com.example.project.payLoad.ProductDTO;
import com.example.project.payLoad.ProductResponse;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface ProductService {
    ProductDTO addProduct(ProductDTO productDTO, Long categoryId);

    ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sort_by, String sort_order, String keyword, String category);

    ProductResponse searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sort_by, String sort_order);

    ProductResponse searchByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sort_by, String sort_order);

    ProductDTO update(ProductDTO productDTO, Long productId);

    ProductDTO deleteProduct(Long productId);

    ProductDTO updateImage(Long productId, MultipartFile image) throws IOException;

    ProductResponse getAllProductsForAdmin(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    ProductResponse getAllProductsForSeller(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
}
