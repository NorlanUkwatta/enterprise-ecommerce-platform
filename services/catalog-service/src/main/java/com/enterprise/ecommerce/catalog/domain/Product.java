package com.enterprise.ecommerce.catalog.domain;

import com.enterprise.ecommerce.commons.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Product Aggregate Root in {@code catalog-service}.
 *
 * <p><b>Design & Scalability Decisions for 5M Customers:</b>
 * <ul>
 *   <li>Uses {@link UuidV7Generator} time-ordered primary keys so new products append sequentially
 *       to the PostgreSQL B+Tree index and support $O(\log N)$ keyset/cursor pagination.</li>
 *   <li>Uses {@link BigDecimal} mapped to {@code NUMERIC(12, 2)} (never {@code double}/{@code float})
 *       to prevent IEEE 754 floating-point rounding bugs in financial calculations.</li>
 *   <li>Uses {@code FetchType.LAZY} on {@link Category} to prevent N+1 query storms.</li>
 *   <li>Uses {@link Version} optimistic locking to prevent lost updates when multiple sellers/admins
 *       edit product prices concurrently.</li>
 * </ul>
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sku", nullable = false, unique = true, length = 64)
    private String sku;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "popularity_score", nullable = false)
    private long popularityScore;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Product() {
        // Required by JPA
    }

    private Product(Builder builder) {
        this.id = builder.id != null ? builder.id : UuidV7Generator.generate();
        this.sku = Objects.requireNonNull(builder.sku, "Product SKU must not be null");
        this.name = Objects.requireNonNull(builder.name, "Product name must not be null");
        this.description = builder.description;
        this.price = validatePrice(builder.price);
        this.currency = builder.currency != null ? builder.currency : "USD";
        this.category = Objects.requireNonNull(builder.category, "Product category must not be null");
        this.brand = builder.brand;
        this.imageUrl = builder.imageUrl;
        this.popularityScore = builder.popularityScore;
        this.active = builder.active;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static BigDecimal validatePrice(BigDecimal price) {
        Objects.requireNonNull(price, "Product price must not be null");
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Product price must not be negative");
        }
        return price;
    }

    @PrePersist
    void onPrePersist() {
        if (this.id == null) {
            this.id = UuidV7Generator.generate();
        }
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    void onPreUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updatePrice(BigDecimal newPrice) {
        this.price = validatePrice(newPrice);
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public Category getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public long getPopularityScore() {
        return popularityScore;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public static final class Builder {
        private UUID id;
        private String sku;
        private String name;
        private String description;
        private BigDecimal price;
        private String currency = "USD";
        private Category category;
        private String brand;
        private String imageUrl;
        private long popularityScore;
        private boolean active = true;

        private Builder() {
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder sku(String sku) {
            this.sku = sku;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder category(Category category) {
            this.category = category;
            return this;
        }

        public Builder brand(String brand) {
            this.brand = brand;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder popularityScore(long popularityScore) {
            this.popularityScore = popularityScore;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Product build() {
            return new Product(this);
        }
    }
}
