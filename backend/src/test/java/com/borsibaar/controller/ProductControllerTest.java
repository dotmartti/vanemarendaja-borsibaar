package com.borsibaar.controller;

import com.borsibaar.dto.ProductRequestDto;
import com.borsibaar.dto.ProductResponseDto;
import com.borsibaar.entity.Role;
import com.borsibaar.entity.User;
import com.borsibaar.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateProduct_Success() throws Exception {
        // Arrange: Create authenticated user
        User user = createMockUser(1L);
        setupSecurityContextWithUser(user);

        // Arrange: Create request DTO
        ProductRequestDto request = new ProductRequestDto(
                "Test Product",
                "Test Description",
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("20.00"),
                1L);

        // Arrange: Create expected response
        ProductResponseDto response = new ProductResponseDto(
                1L,
                "Test Product",
                "Test Description",
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("20.00"),
                1L,
                "Test Category");

        // Arrange: Mock service
        when(productService.create(any(ProductRequestDto.class), eq(1L)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.currentPrice").value(10.00))
                .andExpect(jsonPath("$.minPrice").value(5.00))
                .andExpect(jsonPath("$.maxPrice").value(20.00))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName").value("Test Category"));

        // Verify service was called
        verify(productService).create(any(ProductRequestDto.class), eq(1L));
    }

    @Test
    void testCreateProduct_WithInvalidData_ReturnsBadRequest() throws Exception {
        // Arrange: Create authenticated user
        User user = createMockUser(1L);
        setupSecurityContextWithUser(user);

        // Arrange: Create invalid request (missing required fields)
        ProductRequestDto invalidRequest = new ProductRequestDto(
                "", // Empty name - should fail validation
                "Description",
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("20.00"),
                1L);

        // Act & Assert: Expect 400 Bad Request due to validation failure
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateProduct_WithDuplicateName_ReturnsConflict() throws Exception {
        // Arrange: Create authenticated user
        User user = createMockUser(1L);
        setupSecurityContextWithUser(user);

        // Arrange: Create request
        ProductRequestDto request = new ProductRequestDto(
                "Duplicate Product",
                "Description",
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("20.00"),
                1L);

        // Arrange: Mock service to throw conflict exception
        when(productService.create(any(ProductRequestDto.class), eq(1L)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Product with name 'Duplicate Product' already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void testCreateProduct_WithInvalidCategory_ReturnsBadRequest() throws Exception {
        // Arrange: Create authenticated user
        User user = createMockUser(1L);
        setupSecurityContextWithUser(user);

        // Arrange: Create request with non-existent category
        ProductRequestDto request = new ProductRequestDto(
                "Test Product",
                "Description",
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("20.00"),
                999L); // Non-existent category

        // Arrange: Mock service to throw not found exception
        when(productService.create(any(ProductRequestDto.class), eq(1L)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Category not found: 999"));

        // Act & Assert
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetProduct_Success() throws Exception {
        // Arrange: Create expected response
        ProductResponseDto response = new ProductResponseDto(
                1L,
                "Test Product",
                "Test Description",
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("20.00"),
                1L,
                "Test Category");

        // Arrange: Mock service
        when(productService.getById(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.currentPrice").value(10.00))
                .andExpect(jsonPath("$.minPrice").value(5.00))
                .andExpect(jsonPath("$.maxPrice").value(20.00))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName").value("Test Category"));

        // Verify service was called
        verify(productService).getById(1L);
    }

    @Test
    void testGetProduct_NotFound() throws Exception {
        // Arrange: Mock service to throw not found exception
        when(productService.getById(999L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found: 999"));

        // Act & Assert
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound());

        // Verify service was called
        verify(productService).getById(999L);
    }

    @Test
    void testDeleteProduct_Success() throws Exception {
        // Act & Assert: DELETE returns 204 No Content
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        // Verify service delete method was called
        verify(productService).delete(1L);
    }

    @Test
    void testDeleteProduct_NotFound() throws Exception {
        // Arrange: Mock service to throw not found exception
        org.mockito.Mockito.doThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Product not found: 999"))
                .when(productService).delete(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/products/999"))
                .andExpect(status().isNotFound());

        // Verify service was called
        verify(productService).delete(999L);
    }

    /**
     * Helper method to create a mock user with organization.
     */
    private User createMockUser(Long organizationId) {
        Role userRole = Role.builder()
                .id(1L)
                .name("USER")
                .build();

        return User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .name("Test User")
                .organizationId(organizationId)
                .role(userRole)
                .build();
    }

    /**
     * Helper method to setup SecurityContext with a mock authenticated user.
     */
    private void setupSecurityContextWithUser(User user) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user,
                null,
                Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
