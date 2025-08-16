package com.supershoppercart.controllers;

import com.supershoppercart.dtos.CreateShopCartRequestDTO;
import com.supershoppercart.dtos.ShareCartRequestDTO;
import com.supershoppercart.dtos.ShopCartDetailDTO;
import com.supershoppercart.enums.SharePermission;
import com.supershoppercart.enums.ShopCartState;
import com.supershoppercart.models.GroceryItem;
import com.supershoppercart.models.ShopCart;
import com.supershoppercart.models.Shopper;
import com.supershoppercart.services.ShopCartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopCartControllerTest {

    @Mock
    private ShopCartService shopCartService;

    @InjectMocks
    private ShopCartController shopCartController;

    private Shopper testShopper;
    private ShopCart dummyCart;
    private ShopCartDetailDTO testCartDto;
    private CreateShopCartRequestDTO validRequest;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        testShopper = new Shopper();
        testShopper.setId("test_shopper_id");
        testShopper.setEmail("shopper@example.com");
        testShopper.setName("Test Shopper");


        dummyCart = new ShopCart();
        dummyCart.setId("cart-123");
        dummyCart.setName("Test Cart");
        dummyCart.setDateKey("2025-01-01");
        dummyCart.setCreatedBy(testShopper.getId());
        dummyCart.setState(ShopCartState.ACTIVE);
        dummyCart.setCreatedAt(new Date());
        dummyCart.setShopperIds(List.of(testShopper.getId()));

        testCartDto = new ShopCartDetailDTO(dummyCart.getId(), dummyCart);

        validRequest = new CreateShopCartRequestDTO();
        validRequest.setDateKey("2025-01-01");
        validRequest.setItems(List.of(new GroceryItem("Milk", "2 liters")));
    }

    // ===== getMyCarts Tests =====
    @Test
    @DisplayName("getMyCarts should return UNAUTHORIZED when shopper is null")
    void getMyCarts_UnauthorizedWhenNullShopper() {
        ResponseEntity<?> response = shopCartController.getMyCarts(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(shopCartService);
    }

    @Test
    @DisplayName("getMyCarts should return list of carts successfully")
    void getMyCarts_ReturnsCarts() throws ExecutionException, InterruptedException {
        when(shopCartService.getShopCartsByShopperId("test_shopper_id")).thenReturn(List.of(testCartDto));

        ResponseEntity<?> response = shopCartController.getMyCarts(testShopper);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> carts = (List<?>) response.getBody();
        assertNotNull(carts);
        assertEquals(1, carts.size());
        verify(shopCartService).getShopCartsByShopperId("test_shopper_id");
    }

    @Test
    @DisplayName("getMyCarts should return 500 on exception")
    void getMyCarts_Exception() throws ExecutionException, InterruptedException {
        when(shopCartService.getShopCartsByShopperId(anyString())).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<?> response = shopCartController.getMyCarts(testShopper);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertTrue(body.get("error").toString().contains("DB error"));
    }

    // ===== createCart Tests =====
    @Test
    @DisplayName("createCart returns UNAUTHORIZED when shopper is null")
    void createCart_Unauthorized() {
        ResponseEntity<?> response = shopCartController.createCart(null, validRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(shopCartService);
    }

    @Test
    @DisplayName("createCart should create successfully")
    void createCart_Success() throws ExecutionException, InterruptedException {
        when(shopCartService.createShopCart(anyString(), anyList(), anyList())).thenReturn(dummyCart);

        ResponseEntity<?> response = shopCartController.createCart(testShopper, validRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertInstanceOf(ShopCartDetailDTO.class, response.getBody());
        verify(shopCartService).createShopCart(eq(validRequest.getDateKey()), anyList(), anyList());
    }

    @Test
    @DisplayName("createCart returns 500 when service throws")
    void createCart_Exception() throws ExecutionException, InterruptedException {
        when(shopCartService.createShopCart(anyString(), anyList(), anyList()))
                .thenThrow(new RuntimeException("Service failure"));

        ResponseEntity<?> response = shopCartController.createCart(testShopper, validRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertTrue(body.get("error").toString().contains("Service failure"));
    }

    // ===== shareCart Tests =====
    @Test
    @DisplayName("shareCart returns Unauthorized when shopper is null")
    void shareCart_Unauthorized() {
        ShareCartRequestDTO dto = new ShareCartRequestDTO();
        dto.setTargetShopperEmail("target@example.com");
        dto.setPermission(SharePermission.VIEW);

        ResponseEntity<?> response = shopCartController.shareCart("cart1", dto, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("shareCart success response")
    void shareCart_Success() throws ExecutionException, InterruptedException {
        ShareCartRequestDTO dto = new ShareCartRequestDTO();
        dto.setTargetShopperEmail("target@example.com");
        dto.setPermission(SharePermission.EDIT);

        when(shopCartService.shareShopCart(anyString(), anyString(), anyString(), any())).thenReturn(true);

        ResponseEntity<?> response = shopCartController.shareCart("cart1", dto, testShopper);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(shopCartService).shareShopCart("cart1", "test_shopper_id", "target@example.com", SharePermission.EDIT);
    }

    @Test
    @DisplayName("shareCart failure returns BadRequest")
    void shareCart_Failure() throws ExecutionException, InterruptedException {
        ShareCartRequestDTO dto = new ShareCartRequestDTO();
        dto.setTargetShopperEmail("target@example.com");
        dto.setPermission(SharePermission.VIEW);

        when(shopCartService.shareShopCart(anyString(), anyString(), anyString(), any())).thenReturn(false);

        ResponseEntity<?> response = shopCartController.shareCart("cart1", dto, testShopper);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("shareCart exception returns 500")
    void shareCart_Exception() throws ExecutionException, InterruptedException {
        ShareCartRequestDTO dto = new ShareCartRequestDTO();
        dto.setTargetShopperEmail("target@example.com");
        dto.setPermission(SharePermission.VIEW);

        when(shopCartService.shareShopCart(anyString(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Share failed"));

        ResponseEntity<?> response = shopCartController.shareCart("cart1", dto, testShopper);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertTrue(body.get("error").toString().contains("Share failed"));
    }

    // ===== removeSharing Tests =====
    @Test
    @DisplayName("removeSharing returns Unauthorized when shopper is null")
    void removeSharing_Unauthorized() {
        ResponseEntity<?> response = shopCartController.removeSharing("cart1", "targetId", null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("removeSharing success returns Ok")
    void removeSharing_Success() throws ExecutionException, InterruptedException {
        when(shopCartService.removeSharing("cart1", "test_shopper_id", "targetId")).thenReturn(true);

        ResponseEntity<?> response = shopCartController.removeSharing("cart1", "targetId", testShopper);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(shopCartService).removeSharing("cart1", "test_shopper_id", "targetId");
    }

    @Test
    @DisplayName("removeSharing failure returns BadRequest")
    void removeSharing_Failure() throws ExecutionException, InterruptedException {
        when(shopCartService.removeSharing("cart1", "test_shopper_id", "targetId")).thenReturn(false);

        ResponseEntity<?> response = shopCartController.removeSharing("cart1", "targetId", testShopper);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("removeSharing exception returns 500")
    void removeSharing_Exception() throws ExecutionException, InterruptedException {
        when(shopCartService.removeSharing(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Removal error"));

        ResponseEntity<?> response = shopCartController.removeSharing("cart1", "targetId", testShopper);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertTrue(body.get("error").toString().contains("Removal error"));
    }
}

