package com.example.project.service;

import com.example.project.globalExceptionHandler.APIexception;
import com.example.project.globalExceptionHandler.ResourceNotFoundException;
import com.example.project.model.Cart;
import com.example.project.model.CartItems;
import com.example.project.model.Product;
import com.example.project.payLoad.CartDTO;
import com.example.project.payLoad.CartItemDTO;
import com.example.project.payLoad.ProductDTO;
import com.example.project.repositories.CartItemRepository;
import com.example.project.repositories.CartRepository;
import com.example.project.repositories.ProductRepository;
import com.example.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CartServiceImplementation implements CartService{

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    private AuthUtil authUtil;

    @Override
    @Transactional
    public CartDTO addProductToCart(Long productId, Integer quantity) {

          if (quantity == null || quantity <= 0) {
              throw new APIexception("Quantity must be greater than zero.");
          }

        //Find existing cart or create one
          Cart cart = createCart();

        //Retrieve product details
          Product product = productRepository.findById(productId)
                  .orElseThrow(() -> new ResourceNotFoundException("product", "productId", productId));

          //Perform Validations
          CartItems cartItem = cartItemRepository.findCartItemByProductIdAndCartId(
                  cart.getCartId(),
                  productId
          );

          if(cartItem != null){
               throw new APIexception("Product " + product.getProductName() + " already exists in the cart!");
          }

          if(product.getQuantity() == 0){
               throw new APIexception(product.getProductName() + " is not availble");
          }

          if(product.getQuantity() < quantity){
            throw new APIexception("Please make an order of the " + product.getProductName() +
                    " less than or equal to the quantity " + product.getQuantity() + ".");
          }

          //Create new cartItem
          CartItems newCartItem = new CartItems();

          newCartItem.setProduct(product);
          newCartItem.setCart(cart);
          newCartItem.setQuantity(quantity);
          newCartItem.setDiscount(product.getDiscount());
          newCartItem.setProductPrice(product.getSpecialPrice());

          //save cartItem
          cartItemRepository.save(newCartItem);

          refreshTotal(cart);

        return buildCartDTO(cart);
    }

    private Cart createCart(){
        Cart userCart = cartRepository.findCartByEmail((authUtil.loggedInEmail()));

        if(userCart != null){
            return userCart;
        }

        Cart cart = new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());

        return cartRepository.save(cart);
    }


    @Override
    public List<CartDTO> getAllCarts() {

        List<Cart> carts = cartRepository.findAll();

        // Returns an empty list rather than throwing. "No carts exist" is a valid
        // result for this query, not a client error, and the old APIexception
        // surfaced it as a 400.
        return carts.stream()
                .map(this::buildCartDTO)
                .toList();
    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(emailId, cartId);

        if(cart == null){
            throw new ResourceNotFoundException("Cart", "cartId", cartId);
        }

        return buildCartDTO(cart);
    }

    @Override
    public CartDTO getLoggedInUserCart() {
        Cart cart = cartRepository.findCartByEmail(authUtil.loggedInEmail());

        if (cart == null) {
            // A user who has never added anything has no cart row. The controller
            // used to call cart.getCartId() on this null and return a 500.
            return new CartDTO();
        }

        return buildCartDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity) {

        String emailId = authUtil.loggedInEmail();
        Cart cart = cartRepository.findCartByEmail(emailId);

        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "email", emailId);
        }

        Long cartId = cart.getCartId();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("product", "productId", productId));

        CartItems cartItems = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if(cartItems == null){
            throw new APIexception("Product " + product.getProductName() + " does not exist in the cart!");
        }

        //Calculate the new quantity
        int newQuantity = cartItems.getQuantity() + quantity;

        //Validation to prevent negative quantities
        if(newQuantity < 0){
            throw new APIexception("The resulting quantity cannot be negative");
        }

        if(newQuantity == 0){
            deleteProductFromCart(cartId, productId);
            refreshTotal(cart);
            return buildCartDTO(cart);
        }

        // Stock is only a limit when increasing. Checking it on the way down meant a
        // customer could not reduce the quantity of an item that had since gone out
        // of stock, leaving them stuck with it in the cart.
        if (newQuantity > cartItems.getQuantity()) {
            if (product.getQuantity() == 0) {
                throw new APIexception(product.getProductName() + " is not availble");
            }
            if (product.getQuantity() < newQuantity) {
                throw new APIexception("Please make an order of the " + product.getProductName() +
                        " less than or equal to the quantity " + product.getQuantity() + ".");
            }
        }

        cartItems.setProductPrice(product.getSpecialPrice());
        cartItems.setQuantity(newQuantity);
        cartItems.setDiscount(product.getDiscount());
        cartItemRepository.save(cartItems);

        refreshTotal(cart);

        return buildCartDTO(cart);
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        CartItems cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if(cartItem == null){
            throw new ResourceNotFoundException("Product", "productId", productId);
        }

        String productName = cartItem.getProduct().getProductName();

        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId, productId);

        refreshTotal(cart);

        return "Product " + productName + " removed from the cart!!";
    }

    @Transactional
    @Override
    public String deleteProductFromLoggedInUserCart(Long productId) {
        String emailId = authUtil.loggedInEmail();
        Cart cart = cartRepository.findCartByEmail(emailId);

        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "email", emailId);
        }

        // Resolves the cart from the session instead of taking it from the URL, so a
        // caller cannot delete items out of somebody else's cart.
        return deleteProductFromCart(cart.getCartId(), productId);
    }

    @Override
    @Transactional
    public void updateProductInCarts(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItems cartItems = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if(cartItems == null){
            throw new APIexception("Product " + product.getProductName() + " not availble in the cart!");
        }

        // Re-price the line to the product's current special price, then let
        // refreshTotal recompute. The old version did two successive subtractions of
        // the line value from the cart total, which drove totals negative.
        cartItems.setProductPrice(product.getSpecialPrice());
        cartItems.setDiscount(product.getDiscount());
        cartItemRepository.save(cartItems);

        refreshTotal(cart);
    }

    @Transactional
    @Override
    public String createOrUpdateCartWithItems(List<CartItemDTO> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new APIexception("No cart items supplied.");
        }

        // Get user's email
        String emailId = authUtil.loggedInEmail();
        // Check if an existing cart is availble or create a new one
        Cart existingCart = cartRepository.findCartByEmail(emailId);
        if (existingCart == null) {
            existingCart = new Cart();
            existingCart.setTotalPrice(0.00);
            existingCart.setUser(authUtil.loggedInUser());
            existingCart = cartRepository.save(existingCart);
        }else {
            // Clear all current items in the existing cart
            cartItemRepository.deleteAllByCartId(existingCart.getCartId());
        }

        // Process each item in the request to add to the cart
        for (CartItemDTO cartItemDTO : cartItems) {
            Long productId = cartItemDTO.getProductId();
            Integer quantity = cartItemDTO.getQuantity();

            if (quantity == null || quantity <= 0) {
                throw new APIexception("Quantity for product " + productId + " must be greater than zero.");
            }

        // Find the product by ID
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

            // Stock was never checked here, so a client could seed its cart with more
            // units than exist and only find out at checkout.
            if (product.getQuantity() < quantity) {
                throw new APIexception("Only " + product.getQuantity() + " unit(s) of "
                        + product.getProductName() + " are available.");
            }

        // Create and save cart item
            CartItems cartItem = new CartItems();
            cartItem.setProduct(product);
            cartItem.setCart(existingCart);
            cartItem.setQuantity(quantity);
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setDiscount(product.getDiscount());
            cartItemRepository.save(cartItem);
        }

        refreshTotal(existingCart);
        return "Cart created/updated with the new items successfully";
    }

    /**
     * Recomputes and persists the cart total from its rows in the database.
     * Safe to call after bulk @Modifying deletes, which do not update the in-memory
     * cartItems collection.
     */
    private void refreshTotal(Cart cart) {
        Double total = cartItemRepository.sumCartTotal(cart.getCartId());
        cart.setTotalPrice(total == null ? 0.00 : total);
        cartRepository.save(cart);
    }

    /**
     * Builds a CartDTO whose ProductDTO.quantity carries the quantity *in the cart*.
     *
     * The previous getCart() did this by writing the cart quantity onto the managed
     * Product entity:
     *     cart.getCartItems().forEach(c -> c.getProduct().setQuantity(c.getQuantity()));
     * Those Product instances are attached to the persistence context, so that
     * overwrote real stock levels in the products table on a plain GET of the cart.
     * Mapping to the DTO first and setting the quantity there keeps the entity clean.
     */
    private CartDTO buildCartDTO(Cart cart) {
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        // Read the rows back rather than relying on the in-memory collection, which
        // can be stale after a bulk delete in the same transaction.
        List<CartItems> items = cartItemRepository.findAllByCartId(cart.getCartId());

        List<ProductDTO> products = new ArrayList<>();
        for (CartItems item : items) {
            ProductDTO productDTO = modelMapper.map(item.getProduct(), ProductDTO.class);
            productDTO.setQuantity(item.getQuantity());
            products.add(productDTO);
        }

        cartDTO.setProducts(products);
        cartDTO.setCartId(cart.getCartId());
        cartDTO.setTotalPrice(cart.getTotalPrice() == null ? 0.00 : cart.getTotalPrice());
        return cartDTO;
    }
}
