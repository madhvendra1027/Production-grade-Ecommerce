package com.example.project.repositories;

import com.example.project.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
   @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    Double getTotalRevenue();

   /**
    * Orders containing at least one product owned by the given seller, paged in the
    * database.
    *
    * getAllSellerOrders previously called findAll(pageable) and filtered the
    * resulting page in memory. That returned wrong pagination metadata - counts and
    * page totals described all orders, not the seller's - and silently hid any of the
    * seller's orders that happened to land on a page where none of the rows matched.
    */
   @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN oi.product p "
           + "WHERE p.user.userId = :sellerId")
   Page<Order> findOrdersBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);
}
