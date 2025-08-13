package com.supershoppercart.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supershoppercart.dtos.CreateShopCartRequestDTO;
import com.supershoppercart.dtos.ShareCartRequestDTO;
import com.supershoppercart.dtos.ShopCartDetailDTO;
import com.supershoppercart.enums.SharePermission;
import com.supershoppercart.enums.ShopCartState;
import com.supershoppercart.models.GroceryItem;
import com.supershoppercart.models.ShopCart;
import com.supershoppercart.models.Shopper;
import com.supershoppercart.services.FirestoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

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
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Shopper mockShopper;
    private CreateShopCartRequestDTO validRequest;

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

        mockMvc = MockMvcBuilders.standaloneSetup(shopCartController).build();
        objectMapper = new ObjectMapper();

        // Setup mock shopper
        mockShopper = new Shopper();
        mockShopper.setId("shopper-123");
        mockShopper.setEmail("test@example.com");

        // Setup valid request
        validRequest = new CreateShopCartRequestDTO();
        validRequest.setName("Weekly Shopping");
        validRequest.setDateKey("2024-01-15");
        validRequest.setItems(new ArrayList<>());
        validRequest.setPublic(false);
        validRequest.setTemplate(false);
    }

    @Test
    @DisplayName("Should create cart successfully with valid request")
    void shouldCreateCartSuccessfullyWithValidRequest() throws Exception {
        // Given
        String mockedId = "cart-123";
        when(firestoreService.saveShopCart(any(ShopCart.class))).thenReturn(mockedId);

        // When
        ResponseEntity<?> response = shopCartController.createCart(mockShopper, validRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(ShopCartDetailDTO.class, response.getBody());

        ShopCartDetailDTO responseDto = (ShopCartDetailDTO) response.getBody();
        assertNotNull(responseDto.getIdentifier());
        assertEquals(validRequest.getName(), responseDto.getName());
        assertEquals(validRequest.getDateKey(), responseDto.getDateKey());

        verify(firestoreService, times(1)).saveShopCart(any(ShopCart.class));
    }

    @Test
    @DisplayName("Should create cart with grocery items")
    void shouldCreateCartWithGroceryItems() throws Exception {
        // Given
        List<GroceryItem> items = Arrays.asList(
                new GroceryItem("Milk", "2 liters"),
                new GroceryItem("Bread", "1 loaf")
        );

        validRequest.setItems(items);
        String expectedCartId = "firestore-generated-id-123";

        when(firestoreService.saveShopCart(any(ShopCart.class))).thenReturn(expectedCartId);

        // When
        ResponseEntity<?> response = shopCartController.createCart(mockShopper, validRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ArgumentCaptor<ShopCart> cartCaptor = ArgumentCaptor.forClass(ShopCart.class);
        verify(firestoreService).saveShopCart(cartCaptor.capture());

        ShopCart savedCart = cartCaptor.getValue();
        assertEquals(2, savedCart.getItems().size());
        assertEquals("Milk", savedCart.getItems().get(0).getDesignation());
        assertEquals("2 liters", savedCart.getItems().get(0).getQuantity());
        assertFalse(savedCart.getItems().get(0).isPurchased());
    }

    @Test
    @DisplayName("Should create public template cart")
    void shouldCreatePublicTemplateCart() throws Exception {
        // Given
        validRequest.setPublic(true);
        validRequest.setTemplate(true);
        String mockedId = "cart-123";
        when(firestoreService.saveShopCart(any(ShopCart.class))).thenReturn(mockedId);

        // When
        ResponseEntity<?> response = shopCartController.createCart(mockShopper, validRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ArgumentCaptor<ShopCart> cartCaptor = ArgumentCaptor.forClass(ShopCart.class);
        verify(firestoreService).saveShopCart(cartCaptor.capture());

        ShopCart savedCart = cartCaptor.getValue();
        assertTrue(savedCart.isPublic());
        assertTrue(savedCart.isTemplate());
    }

    @Test
    @DisplayName("Should set correct cart properties during creation")
    void shouldSetCorrectCartPropertiesDuringCreation() throws Exception {
        // Given
        String mockedId = "cart-123";
        when(firestoreService.saveShopCart(any(ShopCart.class))).thenReturn(mockedId);

        // When
        shopCartController.createCart(mockShopper, validRequest);

        // Then
        ArgumentCaptor<ShopCart> cartCaptor = ArgumentCaptor.forClass(ShopCart.class);
        verify(firestoreService).saveShopCart(cartCaptor.capture());

        ShopCart savedCart = cartCaptor.getValue();

        assertNotNull(savedCart.getId());
        assertEquals(validRequest.getName(), savedCart.getName());
        assertEquals(validRequest.getDateKey(), savedCart.getDateKey());
        assertEquals(List.of(mockShopper.getId()), savedCart.getShopperIds());
        assertEquals(mockShopper.getId(), savedCart.getCreatedBy());
        assertEquals(ShopCartState.ACTIVE, savedCart.getState());
        assertNotNull(savedCart.getCreatedAt());
        assertNotNull(savedCart.getLastModified());
        assertNotNull(savedCart.getLastInteraction());
    }

    @Test
    @DisplayName("Should handle null items list gracefully")
    void shouldHandleNullItemsListGracefully() throws Exception {
        // Given
        validRequest.setItems(null);
        String mockedId = "cart-123";
        when(firestoreService.saveShopCart(any(ShopCart.class))).thenReturn(mockedId);

        // When
        ResponseEntity<?> response = shopCartController.createCart(mockShopper, validRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ArgumentCaptor<ShopCart> cartCaptor = ArgumentCaptor.forClass(ShopCart.class);
        verify(firestoreService).saveShopCart(cartCaptor.capture());

        ShopCart savedCart = cartCaptor.getValue();
        assertNotNull(savedCart.getItems());
        assertTrue(savedCart.getItems().isEmpty());
    }

    @Test
    @DisplayName("Should return 500 when FirestoreService throws exception")
    void shouldReturn500WhenFirestoreServiceThrowsException() throws Exception {
        // Given
        String errorMessage = "Database connection failed";
        doThrow(new RuntimeException(errorMessage))
                .when(firestoreService).saveShopCart(any(ShopCart.class));

        // When
        ResponseEntity<?> response = shopCartController.createCart(mockShopper, validRequest);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertTrue(errorResponse.get("error").contains(errorMessage));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized for null shopper")
    void shouldReturnUnauthorizedForNullShopper() throws Exception {

        ResponseEntity<?> response = shopCartController.createCart(null, validRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(firestoreService, never()).saveShopCart(any());

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should handle invalid request with blank name")
    void shouldHandleInvalidRequestWithBlankName() {
        // Given
        validRequest.setName("");

        // When/Then - This would be handled by Spring's validation framework
        // In a real scenario, @Valid annotation would trigger validation before method execution
        assertDoesNotThrow(() -> {
            shopCartController.createCart(mockShopper, validRequest);
        });
    }

    @Test
    @DisplayName("Should handle request with special characters in name")
    void shouldHandleRequestWithSpecialCharactersInName() throws Exception {
        // Given
        validRequest.setName("Shopping List 🛒 with émojis & symbols @#$%");
        String mockedId = "cart-123";
        when(firestoreService.saveShopCart(any(ShopCart.class))).thenReturn(mockedId);

        // When
        ResponseEntity<?> response = shopCartController.createCart(mockShopper, validRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ArgumentCaptor<ShopCart> cartCaptor = ArgumentCaptor.forClass(ShopCart.class);
        verify(firestoreService).saveShopCart(cartCaptor.capture());

        ShopCart savedCart = cartCaptor.getValue();
        assertEquals(validRequest.getName(), savedCart.getName());
    }

    @Test
    @DisplayName("Should handle very long cart name")
    void shouldHandleVeryLongCartName() throws Exception {
        // Given
        String longName = "A".repeat(1000);
        validRequest.setName(longName);
        String mockedId = "cart-123";
        when(firestoreService.saveShopCart(any(ShopCart.class))).thenReturn(mockedId);

        // When
        ResponseEntity<?> response = shopCartController.createCart(mockShopper, validRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ArgumentCaptor<ShopCart> cartCaptor = ArgumentCaptor.forClass(ShopCart.class);
        verify(firestoreService).saveShopCart(cartCaptor.capture());

        ShopCart savedCart = cartCaptor.getValue();
        assertEquals(longName, savedCart.getName());
    }

    @Test
    @DisplayName("Should maintain data integrity throughout creation process")
    void shouldMaintainDataIntegrityThroughoutCreationProcess() throws Exception {
        // Given
        List<GroceryItem> originalItems = Arrays.asList(
                new GroceryItem("Milk", "2 liters"),
                new GroceryItem("Bread", "1 loaf")
        );
        validRequest.setItems(originalItems);
        validRequest.setPublic(true);
        validRequest.setTemplate(false);
        String mockedId = "cart-123";
        when(firestoreService.saveShopCart(any(ShopCart.class))).thenReturn(mockedId);

        // When
        ResponseEntity<?> response = shopCartController.createCart(mockShopper, validRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ShopCartDetailDTO responseDto = (ShopCartDetailDTO) response.getBody();
        assertNotNull(responseDto);
        assertEquals(validRequest.getName(), responseDto.getName());
        assertEquals(validRequest.getDateKey(), responseDto.getDateKey());
        assertFalse(responseDto.isTemplate());

        // Verify the exact cart that was saved
        ArgumentCaptor<ShopCart> cartCaptor = ArgumentCaptor.forClass(ShopCart.class);
        verify(firestoreService).saveShopCart(cartCaptor.capture());

        ShopCart savedCart = cartCaptor.getValue();
        assertEquals(responseDto.getIdentifier(), savedCart.getId());
        assertEquals(2, savedCart.getItems().size());
        assertEquals(1, savedCart.getShopperIds().size());
        assertEquals(mockShopper.getId(), savedCart.getShopperIds().get(0));

        // Verify grocery items details
        GroceryItem firstItem = savedCart.getItems().get(0);
        assertEquals("Milk", firstItem.getDesignation());
        assertEquals("2 liters", firstItem.getQuantity());
        assertFalse(firstItem.isPurchased());
    }

    @Test
    @DisplayName("Should generate unique IDs for multiple cart creations")
    void shouldGenerateUniqueIdsForMultipleCartCreations() throws Exception {
        // Given
        Set<String> generatedIds = new HashSet<>();
        when(firestoreService.saveShopCart(any(ShopCart.class))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) {
                // Return a new unique ID for each invocation
                return UUID.randomUUID().toString();
            }
        });

        // When - Create multiple carts
        for (int i = 0; i < 10; i++) {
            CreateShopCartRequestDTO request = new CreateShopCartRequestDTO();
            request.setName("Cart " + i);
            request.setDateKey("2024-01-15");

            ResponseEntity<?> response = shopCartController.createCart(mockShopper, request);
            ShopCartDetailDTO dto = (ShopCartDetailDTO) response.getBody();
            assertNotNull(dto);
            generatedIds.add(dto.getIdentifier());
        }

        // Then
        assertEquals(10, generatedIds.size()); // All IDs should be unique
    }

    @Test
    @DisplayName("Should handle shopper with very long ID")
    void shouldHandleShopperWithVeryLongId() throws Exception {
        // Given
        Shopper shopperWithLongId = new Shopper();
        shopperWithLongId.setId("a".repeat(500));
        shopperWithLongId.setEmail("test@example.com");
        String mockCartId = "mock-cart-id";

        when(firestoreService.saveShopCart(any(ShopCart.class))).thenReturn(mockCartId);

        // When
        ResponseEntity<?> response = shopCartController.createCart(shopperWithLongId, validRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ArgumentCaptor<ShopCart> cartCaptor = ArgumentCaptor.forClass(ShopCart.class);
        verify(firestoreService).saveShopCart(cartCaptor.capture());

        ShopCart savedCart = cartCaptor.getValue();
        assertEquals(shopperWithLongId.getId(), savedCart.getCreatedBy());
    }

    @Test
    @DisplayName("Should handle request with empty items list")
    void shouldHandleRequestWithEmptyItemsList() throws Exception {
        // Given
        validRequest.setItems(new ArrayList<>());
        String mockedId = "mock_id_85154";
        when(firestoreService.saveShopCart(any(ShopCart.class))).thenReturn(mockedId);

        // When
        ResponseEntity<?> response = shopCartController.createCart(mockShopper, validRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ArgumentCaptor<ShopCart> cartCaptor = ArgumentCaptor.forClass(ShopCart.class);
        verify(firestoreService).saveShopCart(cartCaptor.capture());

        ShopCart savedCart = cartCaptor.getValue();
        assertNotNull(savedCart.getItems());
        assertTrue(savedCart.getItems().isEmpty());
    }

    @Test
    @DisplayName("Should set creation timestamps correctly")
    void shouldSetCreationTimestampsCorrectly() throws Exception {
        // Given
        String mockedId = "mock_id_85154";
        when(firestoreService.saveShopCart(any(ShopCart.class))).thenReturn(mockedId);
        Date beforeCreation = new Date();

        // When
        shopCartController.createCart(mockShopper, validRequest);

        // Then
        Date afterCreation = new Date();
        ArgumentCaptor<ShopCart> cartCaptor = ArgumentCaptor.forClass(ShopCart.class);
        verify(firestoreService).saveShopCart(cartCaptor.capture());

        ShopCart savedCart = cartCaptor.getValue();
        assertTrue(savedCart.getCreatedAt().compareTo(beforeCreation) >= 0);
        assertTrue(savedCart.getCreatedAt().compareTo(afterCreation) <= 0);
        assertEquals(savedCart.getCreatedAt(), savedCart.getLastModified());
        assertEquals(savedCart.getCreatedAt(), savedCart.getLastInteraction());
    }

    @Test
    @DisplayName("Should handle cart creation with multiple grocery items of same type")
    void shouldHandleCartCreationWithMultipleGroceryItemsOfSameType() throws Exception {
        // Given
        List<GroceryItem> items = Arrays.asList(
                new GroceryItem("Milk", "1 liter"),
                new GroceryItem("Milk", "2 liters"),
                new GroceryItem("Bread", "1 loaf")
        );
        validRequest.setItems(items);
        String mockedId = "mock_id_85154";
        when(firestoreService.saveShopCart(any(ShopCart.class))).thenReturn(mockedId);

        // When
        ResponseEntity<?> response = shopCartController.createCart(mockShopper, validRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ArgumentCaptor<ShopCart> cartCaptor = ArgumentCaptor.forClass(ShopCart.class);
        verify(firestoreService).saveShopCart(cartCaptor.capture());

        ShopCart savedCart = cartCaptor.getValue();
        assertEquals(3, savedCart.getItems().size());

        // Verify that items with same designation but different quantities are preserved
        long milkCount = savedCart.getItems().stream()
                .filter(item -> "Milk".equals(item.getDesignation()))
                .count();
        assertEquals(2, milkCount);
    }

    @Test
    @DisplayName("Should create cart with purchased and unpurchased grocery items")
    void shouldCreateCartWithPurchasedAndUnpurchasedGroceryItems() throws Exception {
        // Given
        List<GroceryItem> items = Arrays.asList(
                new GroceryItem("Milk", "2 liters", false),
                new GroceryItem("Bread", "1 loaf", true),
                new GroceryItem("Eggs", "12 pieces")
        );
        validRequest.setItems(items);
        String mockedId = "mock_Id_999";

        // When
        ResponseEntity<?> response = shopCartController.createCart(mockShopper, validRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ArgumentCaptor<ShopCart> cartCaptor = ArgumentCaptor.forClass(ShopCart.class);
        verify(firestoreService).saveShopCart(cartCaptor.capture());

        ShopCart savedCart = cartCaptor.getValue();
        assertEquals(3, savedCart.getItems().size());

        // Verify purchased status
        assertFalse(savedCart.getItems().get(0).isPurchased()); // Milk
        assertTrue(savedCart.getItems().get(1).isPurchased());  // Bread
        assertFalse(savedCart.getItems().get(2).isPurchased()); // Eggs (default false)
    }

    @Test
    @DisplayName("Should handle grocery items with special characters in designation and quantity")
    void shouldHandleGroceryItemsWithSpecialCharacters() throws Exception {
        // Given
        List<GroceryItem> items = Arrays.asList(
                new GroceryItem("Café au lait ☕", "2 tasses"),
                new GroceryItem("Açaí smoothie", "500ml @ €5.99")
        );
        validRequest.setItems(items);
        String mockedId = "mock_id_85154";
        when(firestoreService.saveShopCart(any(ShopCart.class))).thenReturn(mockedId);

        // When
        ResponseEntity<?> response = shopCartController.createCart(mockShopper, validRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ArgumentCaptor<ShopCart> cartCaptor = ArgumentCaptor.forClass(ShopCart.class);
        verify(firestoreService).saveShopCart(cartCaptor.capture());

        ShopCart savedCart = cartCaptor.getValue();
        assertEquals(2, savedCart.getItems().size());
        assertEquals("Café au lait ☕", savedCart.getItems().get(0).getDesignation());
        assertEquals("2 tasses", savedCart.getItems().get(0).getQuantity());
    }

    @Test
    @DisplayName("Should verify cart ID is valid UUID format")
    void shouldVerifyCartIdIsValidUuidFormat() throws Exception {
        // Given
        String mockCartId = UUID.randomUUID().toString();
        when(firestoreService.saveShopCart(any(ShopCart.class))).thenReturn(mockCartId);

        // When
        ResponseEntity<?> response = shopCartController.createCart(mockShopper, validRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ArgumentCaptor<ShopCart> cartCaptor = ArgumentCaptor.forClass(ShopCart.class);
        verify(firestoreService).saveShopCart(cartCaptor.capture());

        ShopCart savedCart = cartCaptor.getValue();
        assertDoesNotThrow(() -> UUID.fromString(savedCart.getId()));
    }

    @Test
    @DisplayName("Should handle empty designation and quantity in grocery items")
    void shouldHandleEmptyDesignationAndQuantityInGroceryItems() throws Exception {
        // Given
        List<GroceryItem> items = Arrays.asList(
                new GroceryItem("", "2 pieces"),
                new GroceryItem("Milk", ""),
                new GroceryItem("", "")
        );
        validRequest.setItems(items);
        String mockCartId = UUID.randomUUID().toString();
        // Correctly mock the method to return a value
        when(firestoreService.saveShopCart(any(ShopCart.class))).thenReturn(mockCartId);


        // When
        ResponseEntity<?> response = shopCartController.createCart(mockShopper, validRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ArgumentCaptor<ShopCart> cartCaptor = ArgumentCaptor.forClass(ShopCart.class);
        verify(firestoreService).saveShopCart(cartCaptor.capture());

        ShopCart savedCart = cartCaptor.getValue();
        assertEquals(3, savedCart.getItems().size());
        assertEquals("", savedCart.getItems().get(0).getDesignation());
        assertEquals("2 pieces", savedCart.getItems().get(0).getQuantity());
        assertEquals("Milk", savedCart.getItems().get(1).getDesignation());
        assertEquals("", savedCart.getItems().get(1).getQuantity());
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
