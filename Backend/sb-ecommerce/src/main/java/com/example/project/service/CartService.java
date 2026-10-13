package com.example.project.service;

import com.example.project.payLoad.CartDTO;
import com.example.project.payLoad.CartItemDTO;

import java.util.List;

public interface CartService {

    CartDTO addProductToCart(Long productId, Integer quantity);

    /** Every cart in the system. Admin-only. */
    List<CartDTO> getAllCarts();

    CartDTO getCart(String emailId, Long cartId);

    /** The authenticated user's cart, or an empty cart if they do not have one yet. */
    CartDTO getLoggedInUserCart();

    CartDTO updateProductQuantityInCart(Long productId, Integer quantity);

    /**
     * Internal use only - trusts the supplied cartId.
     * Controllers must use deleteProductFromLoggedInUserCart instead.
     */
    String deleteProductFromCart(Long cartId, Long productId);

    /** Removes a product from the authenticated user's own cart. */
    String deleteProductFromLoggedInUserCart(Long productId);

    void updateProductInCarts(Long cartId, Long productId);

    String createOrUpdateCartWithItems(List<CartItemDTO> cartItems);
}
