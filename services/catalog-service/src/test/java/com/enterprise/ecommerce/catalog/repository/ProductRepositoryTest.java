package com.enterprise.ecommerce.catalog.repository;

import com.enterprise.ecommerce.catalog.domain.Category;
import com.enterprise.ecommerce.catalog.domain.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalog_test_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class ProductRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("Should execute Flyway migration V1 and persist Category and Product with UUIDv7 keys")
    void shouldPersistCategoryAndProductWithUuidV7() {
        Category electronics = categoryRepository.saveAndFlush(Category.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Smartphones, Laptops & Accessories")
                .build());

        Product phone = productRepository.saveAndFlush(Product.builder()
                .sku("IPHONE-16-PRO-256")
                .name("iPhone 16 Pro 256GB")
                .description("Flagship titanium smartphone")
                .price(new BigDecimal("1199.99"))
                .category(electronics)
                .brand("Apple")
                .popularityScore(9500L)
                .build());

        assertThat(electronics.getId().version()).isEqualTo(7);
        assertThat(phone.getId().version()).isEqualTo(7);
        assertThat(productRepository.findBySku("IPHONE-16-PRO-256"))
                .isPresent()
                .get()
                .satisfies(found -> {
                    assertThat(found.getName()).isEqualTo("iPhone 16 Pro 256GB");
                    assertThat(found.getPrice()).isEqualByComparingTo("1199.99");
                    assertThat(found.getVersion()).isZero();
                });
    }

    @Test
    @DisplayName("Should paginate active products in O(log N) descending UUIDv7 order using keyset cursor")
    void shouldPaginateProductsUsingUuidV7KeysetCursor() {
        Category category = categoryRepository.saveAndFlush(Category.builder()
                .name("Computing")
                .slug("computing")
                .build());

        Product p1 = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-001")
                .name("Mechanical Keyboard")
                .price(new BigDecimal("129.00"))
                .category(category)
                .build());

        Product p2 = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-002")
                .name("Wireless Gaming Mouse")
                .price(new BigDecimal("89.00"))
                .category(category)
                .build());

        Product p3 = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-003")
                .name("4K OLED Monitor")
                .price(new BigDecimal("799.00"))
                .category(category)
                .build());

        // Page 1: Fetch latest 2 products (no cursor yet -> returns [p3, p2])
        List<Product> firstPage = productRepository.findActiveProductsBeforeCursor(null, PageRequest.of(0, 2));
        assertThat(firstPage).extracting(Product::getSku).containsExactly("SKU-003", "SKU-002");

        // Page 2: Pass last item's UUIDv7 (p2.getId()) as cursor -> returns [p1]
        List<Product> secondPage = productRepository.findActiveProductsBeforeCursor(p2.getId(), PageRequest.of(0, 2));
        assertThat(secondPage).extracting(Product::getSku).containsExactly("SKU-001");
        assertThat(secondPage.getFirst().getId()).isEqualTo(p1.getId());
    }

    @Test
    @DisplayName("Should reject negative product prices at domain construction time")
    void shouldRejectNegativeProductPrice() {
        Category category = Category.builder().name("Books").slug("books").build();

        assertThatThrownBy(() -> Product.builder()
                .sku("BOOK-001")
                .name("Distributed Systems")
                .price(new BigDecimal("-5.00"))
                .category(category)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product price must not be negative");
    }
}
