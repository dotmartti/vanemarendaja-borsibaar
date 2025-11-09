package com.borsibaar.backend.service;

import com.borsibaar.backend.dto.ProductRequestDto;
import com.borsibaar.backend.dto.ProductResponseDto;
import com.borsibaar.backend.entity.Category;
import com.borsibaar.backend.entity.Inventory;
import com.borsibaar.backend.entity.InventoryTransaction;
import com.borsibaar.backend.entity.Product;
import com.borsibaar.backend.mapper.ProductMapper;
import com.borsibaar.backend.repository.CategoryRepository;
import com.borsibaar.backend.repository.InventoryRepository;
import com.borsibaar.backend.repository.InventoryTransactionRepository;
import com.borsibaar.backend.repository.ProductRepository;
import com.borsibaar.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Transactional
    public ProductResponseDto create(ProductRequestDto request, Long orgId) {
        Category cat = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Category not found: " + request.categoryId()));

        Product entity = productMapper.toEntity(request);
        entity.setOrganizationId(orgId);
        entity.setActive(true);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        String normalizedName = entity.getName() != null ? entity.getName().trim() : null;
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name must not be blank");
        }
        entity.setName(normalizedName);

        if (!orgId.equals(cat.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category does not belong to the organization");
        }

        if (productRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, normalizedName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Product with name '" + normalizedName + "' already exists");
        }

        Product saved = productRepository.save(entity);

        // Automatically create inventory record with 0 quantity
        createInitialInventory(saved, orgId);

        ProductResponseDto base = productMapper.toResponse(saved);
        return new ProductResponseDto(
                base.id(),
                base.name(),
                base.description(),
                base.currentPrice(),
                base.categoryId(),
                cat.getName()
        );
    }

    private void createInitialInventory(Product product, Long organizationId) {
        Inventory inventory = new Inventory(organizationId, product.getId(), BigDecimal.ZERO, product.getBasePrice());
        Inventory savedInventory = inventoryRepository.save(inventory);

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setInventory(savedInventory);
        transaction.setTransactionType("INITIAL");
        transaction.setQuantityChange(BigDecimal.ZERO);
        transaction.setQuantityBefore(BigDecimal.ZERO);
        transaction.setQuantityAfter(BigDecimal.ZERO);
        transaction.setPriceBefore(Optional.ofNullable(product.getBasePrice()).orElse(BigDecimal.ZERO));
        transaction.setPriceAfter(Optional.ofNullable(product.getBasePrice()).orElse(BigDecimal.ZERO));
        transaction.setNotes("Product created - initial inventory");
        transaction.setCreatedAt(OffsetDateTime.now());
        inventoryTransactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found: " + id));

        ProductResponseDto base = productMapper.toResponse(product);
        String categoryName = categoryRepository.findById(product.getCategoryId())
                .map(Category::getName)
                .orElse(null);

        return new ProductResponseDto(
                base.id(),
                base.name(),
                base.description(),
                base.currentPrice(),
                base.categoryId(),
                categoryName
        );
    }
}
