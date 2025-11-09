package com.borsibaar.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "products")
@Getter @Setter
public class Product {
    public static final BigDecimal DEFAULT_PRICE_INCREASE = BigDecimal.valueOf(0.05); // TODO define under org
    public static final BigDecimal DEFAULT_PRICE_DECREASE = BigDecimal.valueOf(0.05);
    public static final BigDecimal DEFAULT_MIN_PRICE = BigDecimal.valueOf(0.05);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "base_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal basePrice;

    @Column(name = "min_price", precision = 19, scale = 4)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 19, scale = 4)
    private BigDecimal maxPrice;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToOne(mappedBy = "product", fetch = FetchType.EAGER)
    private Inventory inventory;
}
