package com.example.project.repositories;

import com.example.project.model.CartItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItems, Long> {
    @Query("SELECT ci FROM CartItems ci WHERE ci.cart.cartId = ?1 AND ci.product.productId = ?2")
    CartItems findCartItemByProductIdAndCartId(Long cartId, Long productId);

    @Query("SELECT ci FROM CartItems ci WHERE ci.cart.cartId = ?1")
    List<CartItems> findAllByCartId(Long cartId);

    /**
     * Authoritative cart total, computed in the database.
     *
     * The previous code adjusted Cart.totalPrice incrementally on every add, update
     * and delete. Any missed or double-counted adjustment - and a price change
     * between adding an item and changing its quantity - left the stored total
     * permanently out of step with the items. Recomputing from the rows is
     * self-healing.
     */
    @Query("SELECT COALESCE(SUM(ci.productPrice * ci.quantity), 0) FROM CartItems ci WHERE ci.cart.cartId = ?1")
    Double sumCartTotal(Long cartId);

    @Modifying
    @Query("DELETE FROM CartItems ci WHERE ci.cart.cartId = ?1 AND ci.product.productId = ?2")
    void deleteCartItemByProductIdAndCartId(Long cartId, Long productId);

    @Modifying
    @Query("DELETE FROM CartItems ci WHERE ci.cart.cartId = ?1")
    void deleteAllByCartId(Long cartId);
}
