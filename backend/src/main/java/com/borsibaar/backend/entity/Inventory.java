package com.borsibaar.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * TODO: remove duplicate organizationId (already in products table)
     */
    @Deprecated
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "product_id", nullable = false, insertable = false, updatable = false)
    private Long productId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "adjusted_price", precision = 19, scale = 4)
    private BigDecimal adjustedPrice;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    @OneToMany(mappedBy = "inventory")
    private Set<InventoryTransaction> transactions = new HashSet<>();

    // Custom constructor for easy creation
    public Inventory(Long organizationId, Long productId, BigDecimal quantity, BigDecimal adjustedPrice) {
        this.organizationId = organizationId;
        this.productId = productId;
        this.quantity = quantity;
        this.adjustedPrice = adjustedPrice;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }
}
