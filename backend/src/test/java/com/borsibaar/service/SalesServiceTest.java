package com.borsibaar.service;

import com.borsibaar.dto.SaleItemRequestDto;
import com.borsibaar.dto.SaleRequestDto;
import com.borsibaar.dto.SaleResponseDto;
import com.borsibaar.entity.Inventory;
import com.borsibaar.entity.InventoryTransaction;
import com.borsibaar.entity.Product;
import com.borsibaar.repository.InventoryRepository;
import com.borsibaar.repository.InventoryTransactionRepository;
import com.borsibaar.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesServiceTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private SalesService salesService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void processSale_SingleItem_SuccessPriceIncreaseCapped() {
        Product product = new Product(); product.setId(5L); product.setOrganizationId(1L); product.setActive(true); product.setBasePrice(BigDecimal.valueOf(10)); product.setMaxPrice(BigDecimal.valueOf(10)); product.setName("Beer");
        Inventory inventory = new Inventory(); inventory.setId(9L); inventory.setProduct(product); inventory.setProductId(5L); inventory.setOrganizationId(1L); inventory.setQuantity(BigDecimal.valueOf(20)); inventory.setAdjustedPrice(BigDecimal.valueOf(10)); inventory.setUpdatedAt(OffsetDateTime.now());
        product.setInventory(inventory);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);
        when(inventoryTransactionRepository.save(any(InventoryTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        SaleItemRequestDto item = new SaleItemRequestDto(5L, BigDecimal.valueOf(2));
        SaleRequestDto request = new SaleRequestDto(List.of(item), "note", 1L);
        SaleResponseDto response = salesService.processSale(request, userId, 1L);
        assertEquals(1, response.items().size());
        assertEquals(BigDecimal.valueOf(20), response.totalAmount());
        // Price capped at max (10)
        assertEquals(BigDecimal.valueOf(10), inventory.getAdjustedPrice());
        verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    void processSale_InsufficientStock_Throws() {
        Product product = new Product(); product.setId(5L); product.setOrganizationId(1L); product.setActive(true); product.setBasePrice(BigDecimal.ONE); product.setName("Beer");
        Inventory inventory = new Inventory(); inventory.setId(9L); inventory.setProduct(product); inventory.setProductId(5L); inventory.setOrganizationId(1L); inventory.setQuantity(BigDecimal.ONE); inventory.setAdjustedPrice(BigDecimal.ONE);
        product.setInventory(inventory);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        SaleItemRequestDto item = new SaleItemRequestDto(5L, BigDecimal.valueOf(5));
        SaleRequestDto request = new SaleRequestDto(List.of(item), null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> salesService.processSale(request, userId, 1L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void processSale_ProductInactive_Throws() {
        Product product = new Product(); product.setId(5L); product.setOrganizationId(1L); product.setActive(false); product.setBasePrice(BigDecimal.ONE); product.setName("Beer");
        Inventory inventory = new Inventory(); inventory.setId(9L); inventory.setProduct(product); inventory.setProductId(5L); inventory.setOrganizationId(1L); inventory.setQuantity(BigDecimal.ONE); inventory.setAdjustedPrice(BigDecimal.ONE);
        product.setInventory(inventory);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        SaleItemRequestDto item = new SaleItemRequestDto(5L, BigDecimal.ONE);
        SaleRequestDto request = new SaleRequestDto(List.of(item), null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> salesService.processSale(request, userId, 1L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void processSale_ProductOrgMismatch_Throws() {
        Product product = new Product(); product.setId(5L); product.setOrganizationId(2L); product.setActive(true); product.setBasePrice(BigDecimal.ONE); product.setName("Beer");
        Inventory inventory = new Inventory(); inventory.setId(9L); inventory.setProduct(product); inventory.setProductId(5L); inventory.setOrganizationId(2L); inventory.setQuantity(BigDecimal.ONE); inventory.setAdjustedPrice(BigDecimal.ONE);
        product.setInventory(inventory);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        SaleItemRequestDto item = new SaleItemRequestDto(5L, BigDecimal.ONE);
        SaleRequestDto request = new SaleRequestDto(List.of(item), null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> salesService.processSale(request, userId, 1L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void processSale_ProductInventoryMissing_Throws() {
        Product product = new Product(); product.setId(5L); product.setOrganizationId(1L); product.setActive(true); product.setBasePrice(BigDecimal.ONE); product.setName("Beer");
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        SaleItemRequestDto item = new SaleItemRequestDto(5L, BigDecimal.ONE);
        SaleRequestDto request = new SaleRequestDto(List.of(item), null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> salesService.processSale(request, userId, 1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
