package com.borsibaar.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_id", nullable = false, insertable = false, updatable = false)
    private Long inventoryId;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;

    @Column(name = "quantity_change", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityChange;

    @Column(name = "quantity_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityBefore;

    @Column(name = "quantity_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityAfter;

    @Column(name = "price_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal priceBefore;

    @Column(name = "price_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal priceAfter;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;
}
