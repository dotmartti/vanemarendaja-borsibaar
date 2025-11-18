package com.borsibaar.service;

import com.borsibaar.dto.AddStockRequestDto;
import com.borsibaar.dto.InventoryResponseDto;
import com.borsibaar.dto.RemoveStockRequestDto;
import com.borsibaar.dto.AdjustStockRequestDto;
import com.borsibaar.entity.Inventory;
import com.borsibaar.entity.InventoryTransaction;
import com.borsibaar.entity.Product;
import com.borsibaar.mapper.InventoryMapper;
import com.borsibaar.repository.BarStationRepository;
import com.borsibaar.repository.InventoryRepository;
import com.borsibaar.repository.InventoryTransactionRepository;
import com.borsibaar.repository.ProductRepository;
import com.borsibaar.repository.UserRepository;
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
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BarStationRepository barStationRepository;
    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks private InventoryService inventoryService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void addStock_CreatesInventoryIfMissing() {
        Product product = new Product(); product.setId(5L); product.setOrganizationId(1L); product.setActive(true); product.setBasePrice(BigDecimal.valueOf(2));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByOrganizationIdAndProductId(1L, 5L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> { Inventory i = inv.getArgument(0); i.setId(77L); return i; });
        when(inventoryMapper.toResponse(any())).thenAnswer(inv -> {
            Inventory i = inv.getArgument(0); return new InventoryResponseDto(i.getId(), i.getOrganizationId(), i.getProductId(), "P", i.getQuantity(), i.getAdjustedPrice(), null, null, null, i.getUpdatedAt().toString()); });

        AddStockRequestDto request = new AddStockRequestDto(5L, BigDecimal.valueOf(10), "Notes");
        InventoryResponseDto dto = inventoryService.addStock(request, userId, 1L);
        assertEquals(BigDecimal.valueOf(10), dto.quantity());
        verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    void addStock_ProductInactive_ThrowsGone() {
        Product product = new Product(); product.setId(5L); product.setOrganizationId(1L); product.setActive(false);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        AddStockRequestDto request = new AddStockRequestDto(5L, BigDecimal.ONE, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> inventoryService.addStock(request, userId, 1L));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    @Test
    void removeStock_Insufficient_ThrowsBadRequest() {
        Product product = new Product(); product.setId(5L); product.setOrganizationId(1L); product.setActive(true); product.setBasePrice(BigDecimal.ONE);
        Inventory inv = new Inventory(); inv.setId(9L); inv.setOrganizationId(1L); inv.setProduct(product); inv.setProductId(5L); inv.setQuantity(BigDecimal.valueOf(2)); inv.setAdjustedPrice(BigDecimal.ONE); inv.setUpdatedAt(OffsetDateTime.now());
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByOrganizationIdAndProductId(1L, 5L)).thenReturn(Optional.of(inv));
        RemoveStockRequestDto request = new RemoveStockRequestDto(5L, BigDecimal.valueOf(5), null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> inventoryService.removeStock(request, userId, 1L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void adjustStock_Success_CreatesTransaction() {
        Product product = new Product(); product.setId(5L); product.setOrganizationId(1L); product.setActive(true); product.setBasePrice(BigDecimal.valueOf(2));
        Inventory inv = new Inventory(); inv.setId(9L); inv.setOrganizationId(1L); inv.setProduct(product); inv.setProductId(5L); inv.setQuantity(BigDecimal.valueOf(5)); inv.setAdjustedPrice(BigDecimal.valueOf(2)); inv.setUpdatedAt(OffsetDateTime.now());
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByOrganizationIdAndProductId(1L, 5L)).thenReturn(Optional.of(inv));
        when(inventoryMapper.toResponse(any())).thenAnswer(a -> {
            Inventory i = a.getArgument(0); return new InventoryResponseDto(i.getId(), i.getOrganizationId(), i.getProductId(), "Prod", i.getQuantity(), i.getAdjustedPrice(), null, null, null, i.getUpdatedAt().toString()); });

        AdjustStockRequestDto request = new AdjustStockRequestDto(5L, BigDecimal.valueOf(8), "Adj");
        InventoryResponseDto dto = inventoryService.adjustStock(request, userId, 1L);
        assertEquals(BigDecimal.valueOf(8), dto.quantity());
        verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    void getByOrganization_FiltersInactiveProducts() {
        Inventory inv1 = new Inventory(); inv1.setId(1L); inv1.setOrganizationId(1L); inv1.setProductId(10L); inv1.setQuantity(BigDecimal.ONE); inv1.setUpdatedAt(OffsetDateTime.now());
        Inventory inv2 = new Inventory(); inv2.setId(2L); inv2.setOrganizationId(1L); inv2.setProductId(11L); inv2.setQuantity(BigDecimal.ONE); inv2.setUpdatedAt(OffsetDateTime.now());
        when(inventoryRepository.findByOrganizationId(1L)).thenReturn(List.of(inv1, inv2));
        Product p1 = new Product(); p1.setId(10L); p1.setActive(true); p1.setBasePrice(BigDecimal.ONE); p1.setName("A");
        Product p2 = new Product(); p2.setId(11L); p2.setActive(false); p2.setBasePrice(BigDecimal.ONE); p2.setName("B");
        when(productRepository.findById(10L)).thenReturn(Optional.of(p1));
        when(productRepository.findById(11L)).thenReturn(Optional.of(p2));
        when(inventoryMapper.toResponse(inv1)).thenReturn(new InventoryResponseDto(1L,1L,10L,"A",BigDecimal.ONE,BigDecimal.ONE,null,null,null,OffsetDateTime.now().toString()));
        List<InventoryResponseDto> result = inventoryService.getByOrganization(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getByProductAndOrganization_ProductInactive_Gone() {
        Inventory inv = new Inventory(); inv.setId(1L); inv.setOrganizationId(1L); inv.setProductId(10L); inv.setQuantity(BigDecimal.ONE); inv.setUpdatedAt(OffsetDateTime.now());
        when(inventoryRepository.findByOrganizationIdAndProductId(1L, 10L)).thenReturn(Optional.of(inv));
        Product p = new Product(); p.setId(10L); p.setActive(false); p.setBasePrice(BigDecimal.ONE); p.setName("A");
        when(productRepository.findById(10L)).thenReturn(Optional.of(p));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> inventoryService.getByProductAndOrganization(10L, 1L));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }
}
