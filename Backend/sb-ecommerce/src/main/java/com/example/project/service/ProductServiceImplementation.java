package com.example.project.service;

import com.example.project.globalExceptionHandler.APIexception;
import com.example.project.globalExceptionHandler.ResourceNotFoundException;
import com.example.project.model.Cart;
import com.example.project.model.Category;
import com.example.project.model.Product;
import com.example.project.model.User;
import com.example.project.payLoad.CartDTO;
import com.example.project.payLoad.ProductDTO;
import com.example.project.payLoad.ProductResponse;
import com.example.project.repositories.CartRepository;
import com.example.project.repositories.CategoryRepositories;
import com.example.project.repositories.ProductRepository;
import com.example.project.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class ProductServiceImplementation implements ProductService{

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartService cartService;
    @Autowired
    private CategoryRepositories categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Value("${project.image}")
    private String path;

    @Value("${image.base.url}")
    private String imageBaseUrl;

    @Autowired
    private AuthUtil authUtil;

    @Override
    public ProductDTO addProduct(ProductDTO productDTO, Long categoryId){
         Category category = categoryRepository.findById(categoryId)
                 .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

         boolean isProductNotPresent = true;
         List<Product> products = category.getProducts();
         for(Product value : products){
             if(value.getProductName().equals(productDTO.getProductName())){
                 isProductNotPresent = false;
             }
         }

         if(isProductNotPresent){
             Product product = modelMapper.map(productDTO, Product.class);
             // Ignore any productId on the request body so a create cannot overwrite
             // an existing row.
             product.setProductId(null);
             product.setImage("default.png");
             product.setCategory(category);
             product.setUser(authUtil.loggedInUser());
             product.setSpecialPrice(calculateSpecialPrice(product.getPrice(), product.getDiscount()));
             Product savedProduct = productRepository.save(product);
             return modelMapper.map(savedProduct, ProductDTO.class);
         }else{
              throw new APIexception(("Product already exists."));
         }
    }

    /**
     * Single place where the discounted price is worked out, so add and update cannot
     * drift apart. Discount is a percentage.
     */
    private double calculateSpecialPrice(double price, double discount) {
        double clampedDiscount = Math.max(0.0, Math.min(discount, 100.0));
        return price - (clampedDiscount * 0.01 * price);
    }

    @Override
    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sort_by, String sort_order, String keyword, String category) {
        Sort sortByOrder = sort_order.equalsIgnoreCase("asc")
                         ? Sort.by(sort_by).ascending()
                         : Sort.by(sort_by).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByOrder);
        Specification<Product> spec = Specification.where((Specification<Product>) null);
        if(keyword != null && !keyword.isEmpty()){
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%"));
        }

        if(category != null && !category.isEmpty()){
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(root.get("category").get("categoryName").as(String.class), category));
        }

        Page<Product> productPage = productRepository.findAll(spec, pageDetails);

        List<Product> products = productPage.getContent();

        List<ProductDTO> productDTO = products.stream()
                .map(product -> {
                   ProductDTO productDTOS = modelMapper.map(product, ProductDTO.class);
                   productDTOS.setImage(constructImageUrl((product.getImage())));
                   return productDTOS;
                })
                .toList();
//
       //No of products == 0
//        if(products.isEmpty()){
//            throw new APIexception("No product exists");
//        }

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTO);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }

    private String constructImageUrl(String imageName) {
        if (imageName == null || imageName.isBlank()) {
            return imageName;
        }

        // Cloudinary images already have complete URLs.
        if (imageName.startsWith("https://")
                || imageName.startsWith("http://")) {
            return imageName;
        }

        // Local images use the backend's image URL.
        return imageBaseUrl.endsWith("/")
                ? imageBaseUrl + imageName
                : imageBaseUrl + "/" + imageName;
    }

    @Override
    public ProductResponse searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sort_by, String sort_order) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        Sort sortByOrder = sort_order.equalsIgnoreCase("asc")
                ? Sort.by(sort_by).ascending()
                : Sort.by(sort_by).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByOrder);
        Page<Product> productPage = productRepository.findByCategoryOrderByPriceAsc(category, pageDetails);

        List<Product> products = productPage.getContent();

        List<ProductDTO> productDTOS = products.stream()
                .map(product -> {
                    ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
                    productDTO.setImage(constructImageUrl(product.getImage()));
                    return productDTO;
                })
                .toList();

        //No of products == 0
        // No longer throws when a category happens to be empty. These are public
        // browse endpoints, and an empty result is a normal outcome, not a 400.

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent((productDTOS));
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }

    @Override
    public ProductResponse searchByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sort_by, String sort_order) {
        Sort sortByOrder = sort_order.equalsIgnoreCase("asc")
                ? Sort.by(sort_by).ascending()
                : Sort.by(sort_by).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByOrder);
        Page<Product> productPage = productRepository.findByProductNameLikeIgnoreCase('%' + keyword + '%', pageDetails);

        List<Product> products = productPage.getContent();

        List<ProductDTO> productDTOS = products.stream()
                 .map(product -> {
                     ProductDTO productDTO =  modelMapper.map(product, ProductDTO.class);
                     productDTO.setImage(constructImageUrl(product.getImage()));
                         return productDTO;
                 })
                .toList();

        //No of products == 0
        // Same as above: an empty search result is not a client error.

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent((productDTOS));
        // These five were never set on this response, so keyword searches came back
        // with null/0 pagination metadata and the UI could not page through results.
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }

    @Override
    public ProductDTO update(ProductDTO productDTO, Long productId) {
        //to get the product to be updated from the database.
        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(()->new ResourceNotFoundException("Product", "productId", productId));

        //update the fields of the product from the database with the provided product's info.
        Product product = modelMapper.map(productDTO, Product.class);
        productFromDB.setProductName(product.getProductName());
        productFromDB.setDescription(product.getDescription());
        productFromDB.setQuantity(product.getQuantity());
        productFromDB.setDiscount(product.getDiscount());
        productFromDB.setPrice(product.getPrice());

        // specialPrice is derived, never accepted from the request. It used to be
        // copied straight off the DTO, so a caller could set a product's price to
        // 100 and its special price to 0 in the same request.
        productFromDB.setSpecialPrice(calculateSpecialPrice(product.getPrice(), product.getDiscount()));

        //save the updated product
        Product savedProduct = productRepository.save(productFromDB);

        // Re-price this product in every cart that holds it. The old version mapped
        // each cart into a CartDTO, including every ProductDTO in it, purely to read
        // back the cart id it already had.
        cartRepository.findCartsByProductId(productId)
                .forEach(cart -> cartService.updateProductInCarts(cart.getCartId(), productId));

        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    @Override
    public ProductDTO deleteProduct(Long productId) {
        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(()->new ResourceNotFoundException("Product", "productId", productId));

        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        carts.forEach(cart -> cartService.deleteProductFromCart(cart.getCartId(), productId));

        productRepository.delete(productFromDB);
        return modelMapper.map(productFromDB, ProductDTO.class);
    }

    @Override
    public ProductDTO updateImage(Long productId, MultipartFile image) throws IOException {
        //Get the product from Database.
        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        //Upload image to the server
        //get the filename of uploaded image.
        String fileName = fileService.uploadImage(path, image);

        //updating the new file name to the product.
        productFromDB.setImage(fileName);

        //Save the product
        Product updatedProduct = productRepository.save(productFromDB);

        //return productDTO after mapping product to DTO.
        return modelMapper.map(updatedProduct, ProductDTO.class);
    }

    @Override
    public ProductResponse getAllProductsForAdmin(Integer pageNumber, Integer pageSize, String sort_by, String sort_order) {
            Sort sortByOrder = sort_order.equalsIgnoreCase("asc")
                    ? Sort.by(sort_by).ascending()
                    : Sort.by(sort_by).descending();

            Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByOrder);
            Page<Product> productPage = productRepository.findAll(pageDetails);

            List<Product> products = productPage.getContent();

            List<ProductDTO> productDTO = products.stream()
                    .map(product -> {
                        ProductDTO productDTOS = modelMapper.map(product, ProductDTO.class);
                        productDTOS.setImage(constructImageUrl((product.getImage())));
                        return productDTOS;
                    })
                    .toList();
//
            //No of products == 0
//        if(products.isEmpty()){
//            throw new APIexception("No product exists");
//        }

            ProductResponse productResponse = new ProductResponse();
            productResponse.setContent(productDTO);
            productResponse.setPageNumber(productPage.getNumber());
            productResponse.setPageSize(productPage.getSize());
            productResponse.setTotalPages(productPage.getTotalPages());
            productResponse.setTotalElements(productPage.getTotalElements());
            productResponse.setLastPage(productPage.isLast());
            return productResponse;
    }

    @Override
    public ProductResponse getAllProductsForSeller(Integer pageNumber, Integer pageSize, String sort_by, String sort_order) {
        Sort sortByOrder = sort_order.equalsIgnoreCase("asc")
                ? Sort.by(sort_by).ascending()
                : Sort.by(sort_by).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByOrder);

        User user = authUtil.loggedInUser();
        Page<Product> productPage = productRepository.findByUser(user, pageDetails);

        List<Product> products = productPage.getContent();

        List<ProductDTO> productDTO = products.stream()
                .map(product -> {
                    ProductDTO productDTOS = modelMapper.map(product, ProductDTO.class);
                    productDTOS.setImage(constructImageUrl((product.getImage())));
                    return productDTOS;
                })
                .toList();
//
        //No of products == 0
//        if(products.isEmpty()){
//            throw new APIexception("No product exists");
//        }

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTO);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }
}
