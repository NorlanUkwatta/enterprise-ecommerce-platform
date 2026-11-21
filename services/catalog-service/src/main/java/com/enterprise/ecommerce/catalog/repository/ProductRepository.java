package com.enterprise.ecommerce.catalog.repository;

import com.enterprise.ecommerce.catalog.domain.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for {@link Product}.
 *
 * <p><b>Why Keyset / Cursor Pagination Instead of OFFSET Pagination at 5M Scale:</b>
 * Traditional {@code LIMIT 20 OFFSET 500000} forces PostgreSQL to scan and discard 500,000 index rows
 * in $O(N)$ linear time. Because our {@code id} column uses time-ordered <b>UUIDv7</b>, we use
 * Keyset Pagination ({@code WHERE p.active = true AND p.id < :cursorId ORDER BY p.id DESC}), which
 * jumps directly to the cursor position in the B+Tree index in $O(\log N)$ time regardless of page depth!
 */
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    @Query("""
            SELECT p FROM Product p
            JOIN FETCH p.category
            WHERE p.active = true
              AND (:cursorId IS NULL OR p.id < :cursorId)
            ORDER BY p.id DESC
            """)
    List<Product> findActiveProductsBeforeCursor(
            @Param("cursorId") UUID cursorId,
            Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            JOIN FETCH p.category
            WHERE p.category.id = :categoryId
              AND p.active = true
              AND (:cursorId IS NULL OR p.id < :cursorId)
            ORDER BY p.id DESC
            """)
    List<Product> findActiveProductsByCategoryBeforeCursor(
            @Param("categoryId") UUID categoryId,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);
}
