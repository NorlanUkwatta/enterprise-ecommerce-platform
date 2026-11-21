package com.enterprise.ecommerce.catalog.domain;

import com.enterprise.ecommerce.commons.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Category Aggregate Root in {@code catalog-service}.
 * Uses time-ordered UUIDv7 primary keys and JPA {@link Version} optimistic locking.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 140)
    private String slug;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Category() {
        // Required by JPA
    }

    private Category(Builder builder) {
        this.id = builder.id != null ? builder.id : UuidV7Generator.generate();
        this.name = Objects.requireNonNull(builder.name, "Category name must not be null");
        this.slug = Objects.requireNonNull(builder.slug, "Category slug must not be null");
        this.description = builder.description;
        this.active = builder.active;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Builder builder() {
        return new Builder();
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

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
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

    public void updateDetails(String name, String description, boolean active) {
        this.name = Objects.requireNonNull(name, "Category name must not be null");
        this.description = description;
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public static final class Builder {
        private UUID id;
        private String name;
        private String slug;
        private String description;
        private boolean active = true;

        private Builder() {
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder slug(String slug) {
            this.slug = slug;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Category build() {
            return new Category(this);
        }
    }
}
