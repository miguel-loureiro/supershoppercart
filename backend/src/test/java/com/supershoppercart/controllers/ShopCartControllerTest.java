package com.supershoppercart.controllers;

import com.supershoppercart.dtos.ShareCartRequestDTO;
import com.supershoppercart.dtos.ShopCartDetailDTO;
import com.supershoppercart.enums.SharePermission;
import com.supershoppercart.models.ShopCart;
import com.supershoppercart.models.Shopper;
import com.supershoppercart.services.FirestoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopCartControllerTest {

    @Mock
    private FirestoreService firestoreService;

    @InjectMocks
    private ShopCartController shopCartController;

    private Shopper testShopper;
    private ShopCartDetailDTO testCartDto;

    @BeforeEach
    void setUp() {
        testShopper = new Shopper("shopper@example.com", "Test Shopper");
        testShopper.setId("test_shopper_id");

        ShopCart dummyShopCart = new ShopCart();
        String randomIdentifier = UUID.randomUUID().toString();
        dummyShopCart.setId(randomIdentifier);
        dummyShopCart.setName("Test Cart");

        testCartDto = new ShopCartDetailDTO(randomIdentifier, dummyShopCart);
        testCartDto.setShoppers(Collections.emptyList());
    }

    // --- getMyCarts Endpoint Tests ---

    @Test
    void getMyCarts_ReturnsListOfCartsSuccessfully() throws Exception {
        // Arrange
        List<ShopCartDetailDTO> expectedCarts = List.of(testCartDto);
        when(firestoreService.getShopCartsByShopperId("test_shopper_id")).thenReturn(expectedCarts);

        // Act
        List<ShopCartDetailDTO> actualCarts = shopCartController.getMyCarts(testShopper);

        // Assert
        assertNotNull(actualCarts);
        assertEquals(1, actualCarts.size());
        assertEquals(expectedCarts.get(0).getIdentifier(), actualCarts.get(0).getIdentifier());
        verify(firestoreService, times(1)).getShopCartsByShopperId("test_shopper_id");
    }

    @Test
    void getMyCarts_ServiceThrowsException_ThrowsException() {
        // Arrange
        when(firestoreService.getShopCartsByShopperId("test_shopper_id"))
                .thenThrow(new RuntimeException("Firestore error"));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            shopCartController.getMyCarts(testShopper);
        });

        assertEquals("Firestore error", thrown.getMessage());
        verify(firestoreService, times(1)).getShopCartsByShopperId("test_shopper_id");
    }

    // --- shareCart Endpoint Tests ---

    @Test
    void shareCart_SuccessfulSharing_ReturnsOkResponse() throws Exception {
        // Arrange
        Authentication mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal()).thenReturn(testShopper);

        String cartId = testCartDto.getIdentifier();
        ShareCartRequestDTO requestDto = new ShareCartRequestDTO();
        requestDto.setTargetShopperEmail("target@example.com");
        requestDto.setPermission(SharePermission.EDIT);

        when(firestoreService.shareCartWithShopper(cartId, "test_shopper_id", requestDto.getTargetShopperEmail(), requestDto.getPermission()))
                .thenReturn(true);

        // Act
        ResponseEntity<?> response = shopCartController.shareCart(cartId, requestDto, mockAuthentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Map.of("message", "Cart shared successfully"), response.getBody());
        verify(firestoreService, times(1)).shareCartWithShopper(cartId, "test_shopper_id", requestDto.getTargetShopperEmail(), requestDto.getPermission());
    }

    @Test
    void shareCart_SharingFails_ReturnsBadRequest() throws Exception {
        // Arrange
        Authentication mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal()).thenReturn(testShopper);

        String cartId = testCartDto.getIdentifier();
        ShareCartRequestDTO requestDto = new ShareCartRequestDTO();
        requestDto.setTargetShopperEmail("target@example.com");
        requestDto.setPermission(SharePermission.EDIT);

        when(firestoreService.shareCartWithShopper(cartId, "test_shopper_id", requestDto.getTargetShopperEmail(), requestDto.getPermission()))
                .thenReturn(false);

        // Act
        ResponseEntity<?> response = shopCartController.shareCart(cartId, requestDto, mockAuthentication);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("error", "Failed to share cart. Check permissions and target user exists."), response.getBody());
        verify(firestoreService, times(1)).shareCartWithShopper(cartId, "test_shopper_id", requestDto.getTargetShopperEmail(), requestDto.getPermission());
    }

    @Test
    void shareCart_ServiceThrowsException_ReturnsInternalServerError() throws Exception {
        // Arrange
        Authentication mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal()).thenReturn(testShopper);

        String cartId = testCartDto.getIdentifier();
        ShareCartRequestDTO requestDto = new ShareCartRequestDTO();
        requestDto.setTargetShopperEmail("target@example.com");
        requestDto.setPermission(SharePermission.EDIT);

        when(firestoreService.shareCartWithShopper(cartId, "test_shopper_id", requestDto.getTargetShopperEmail(), requestDto.getPermission()))
                .thenThrow(new RuntimeException("Test sharing exception"));

        // Act
        ResponseEntity<?> response = shopCartController.shareCart(cartId, requestDto, mockAuthentication);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(Map.of("error", "Internal server error: Test sharing exception"), response.getBody());
        verify(firestoreService, times(1)).shareCartWithShopper(cartId, "test_shopper_id", requestDto.getTargetShopperEmail(), requestDto.getPermission());
    }

    // --- removeSharing Endpoint Tests ---

    @Test
    void removeSharing_SuccessfulRemoval_ReturnsOkResponse() throws Exception {
        // Arrange
        Authentication mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal()).thenReturn(testShopper);

        String cartId = testCartDto.getIdentifier();
        String targetShopperId = "target_shopper_id";

        when(firestoreService.removeCartSharing(cartId, "test_shopper_id", targetShopperId)).thenReturn(true);

        // Act
        ResponseEntity<?> response = shopCartController.removeSharing(cartId, targetShopperId, mockAuthentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Map.of("message", "Sharing removed successfully"), response.getBody());
        verify(firestoreService, times(1)).removeCartSharing(cartId, "test_shopper_id", targetShopperId);
    }

    @Test
    void removeSharing_RemovalFails_ReturnsBadRequest() throws Exception {
        // Arrange
        Authentication mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal()).thenReturn(testShopper);

        String cartId = testCartDto.getIdentifier();
        String targetShopperId = "target_shopper_id";

        when(firestoreService.removeCartSharing(cartId, "test_shopper_id", targetShopperId)).thenReturn(false);

        // Act
        ResponseEntity<?> response = shopCartController.removeSharing(cartId, targetShopperId, mockAuthentication);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("error", "Failed to remove sharing"), response.getBody());
        verify(firestoreService, times(1)).removeCartSharing(cartId, "test_shopper_id", targetShopperId);
    }

    @Test
    void removeSharing_ServiceThrowsException_ReturnsInternalServerError() throws Exception {
        // Arrange
        Authentication mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal()).thenReturn(testShopper);

        String cartId = testCartDto.getIdentifier();
        String targetShopperId = "target_shopper_id";

        when(firestoreService.removeCartSharing(cartId, "test_shopper_id", targetShopperId))
                .thenThrow(new RuntimeException("Test removal exception"));

        // Act
        ResponseEntity<?> response = shopCartController.removeSharing(cartId, targetShopperId, mockAuthentication);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(Map.of("error", "Internal server error: Test removal exception"), response.getBody());
        verify(firestoreService, times(1)).removeCartSharing(cartId, "test_shopper_id", targetShopperId);
    }
}
