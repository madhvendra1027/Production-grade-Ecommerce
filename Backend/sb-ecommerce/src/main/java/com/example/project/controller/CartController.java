package com.example.project.controller;

import com.example.project.payLoad.CartDTO;
import com.example.project.payLoad.CartItemDTO;
import com.example.project.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CartController {

    @Autowired
    private CartService cartService;

    // AuthUtil and CartRepository are no longer injected here. Resolving the
    // caller's cart is the service layer's job; a controller reaching straight into
    // a repository is how the null-cart 500 below slipped in.

    @PostMapping("/cart/create")
    public ResponseEntity<String> createOrUpdateCart(@RequestBody List<CartItemDTO> cartItems){
        // Note: @Valid on a List parameter does not cascade to its elements in Spring
        // MVC, so per-item checks are enforced in the service instead.
        String response = cartService.createOrUpdateCartWithItems(cartItems);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Every cart in the system, moved from /api/carts to /api/admin/carts.
     *
     * As /api/carts it was reachable by any authenticated user and dumped every
     * customer's cart contents.
     */
    @GetMapping("/admin/carts")
    public ResponseEntity<List<CartDTO>> getCarts(){
        List<CartDTO> cartDTOs = cartService.getAllCarts();
        // Was HttpStatus.FOUND (302) on a successful read, which is a redirect code.
        return new ResponseEntity<>(cartDTOs, HttpStatus.OK);
    }

    @GetMapping("/carts/users/cart")
    public ResponseEntity<CartDTO> getLoggedInUserCart(){
        // Previously: looked up the cart by email in the controller and immediately
        // called cart.getCartId() on it, so any user without a cart row got a 500.
        return new ResponseEntity<>(cartService.getLoggedInUserCart(), HttpStatus.OK);
    }

    @PutMapping("/cart/product/{productId}/quantity/{operation}")
    public ResponseEntity<CartDTO> updateCartProduct(@PathVariable Long productId,
                                                     @PathVariable String operation){
        if (!operation.equalsIgnoreCase("delete") && !operation.equalsIgnoreCase("add")) {
            // Anything that was not "delete" silently counted as an increment.
            return ResponseEntity.badRequest().build();
        }

        CartDTO cartDTO = cartService.updateProductQuantityInCart(productId,
                                operation.equalsIgnoreCase("delete") ? -1 : 1 );

        return new ResponseEntity<>(cartDTO, HttpStatus.OK);
    }

    /**
     * Removes a product from the caller's own cart.
     *
     * The cartId path variable is gone. It was passed straight through to the
     * service, so DELETE /api/carts/{anyCartId}/product/{anyProductId} let any
     * logged-in user empty another user's cart.
     */
    @DeleteMapping("/carts/product/{productId}")
    public ResponseEntity<String> deleteProductFromCart(@PathVariable Long productId){
        String status = cartService.deleteProductFromLoggedInUserCart(productId);
        return new ResponseEntity<>(status, HttpStatus.OK);
    }
}
