package com.enterprise.ecommerce.catalog.repository;

import com.enterprise.ecommerce.catalog.domain.Category;
import com.enterprise.ecommerce.catalog.domain.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full PostgreSQL 16 Testcontainers Integration Test (*IT.java).
 * Automatically runs in GitHub Actions CI and locally whenever Docker Desktop is running;
 * gracefully skips if Docker Desktop is paused locally.
 */
@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.6-alpine");

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("Should validate Flyway schema and keyset cursor pagination on real PostgreSQL 16 container")
    void shouldVerifyFlywayAndCursorPaginationOnRealPostgres() {
        Category audio = categoryRepository.saveAndFlush(Category.builder()
                .name("Audio")
                .slug("audio-postgres-it")
                .description("Headphones and Speakers")
                .build());

        Product first = productRepository.saveAndFlush(Product.builder()
                .sku("SONY-XM5-IT-1")
                .name("Noise Cancelling Headphones Gen 1")
                .price(new BigDecimal("349.99"))
                .category(audio)
                .build());

        Product second = productRepository.saveAndFlush(Product.builder()
                .sku("SONY-XM5-IT-2")
                .name("Noise Cancelling Headphones Gen 2")
                .price(new BigDecimal("399.99"))
                .category(audio)
                .build());

        List<Product> page = productRepository.findActiveProductsByCategoryBeforeCursor(
                audio.getId(), second.getId(), PageRequest.of(0, 10));

        assertThat(page).hasSize(1);
        assertThat(page.getFirst().getId()).isEqualTo(first.getId());
    }
}
