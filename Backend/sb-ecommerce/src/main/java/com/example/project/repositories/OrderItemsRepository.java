package com.example.project.repositories;

import com.example.project.model.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {

    /**
     * True when the order contains at least one product owned by this seller.
     *
     * Used to stop a seller changing the status of an order that has nothing to do
     * with them - the seller status endpoint previously called the same
     * updateOrder(orderId, status) as the admin one with no ownership test at all.
     */
    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END FROM OrderItems oi "
            + "WHERE oi.order.orderId = :orderId AND oi.product.user.userId = :sellerId")
    boolean existsByOrderIdAndSellerId(@Param("orderId") Long orderId,
                                       @Param("sellerId") Long sellerId);
}
