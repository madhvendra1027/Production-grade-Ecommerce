package com.example.project.repositories;

import com.example.project.model.Category;
import com.example.project.model.Product;
import com.example.project.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Page<Product> findByCategoryOrderByPriceAsc(Category category, Pageable pageDetails);

    Optional<Product> findByDummyJsonId(Long dummyJsonId);

    Page<Product> findByProductNameLikeIgnoreCase(String keyword, Pageable pageDetails);

    Page<Product> findByUser(User user, Pageable pageDetails);

    /**
     * Row-locking read used when decrementing stock at checkout.
     *
     * Without it, two concurrent checkouts both read the same quantity, both pass the
     * availability check and both write quantity-n, so the shop can sell more units
     * than it has. PESSIMISTIC_WRITE serialises them (SELECT ... FOR UPDATE), making
     * the second one see the first one's decrement.
     *
     * Must be called inside a transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productId = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") Long productId);
}
